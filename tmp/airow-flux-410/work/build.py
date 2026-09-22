#!/usr/bin/env python3
"""Build the personal test APK using Java 17+, Python 3 and official Android tools.
First run downloads about 145 MB. No Android Studio or emulator is required.
Linux ARM64 also requires system aapt2 and zipalign in /usr/bin.
The Android SDK license applies: https://developer.android.com/studio/terms
"""
import hashlib
import os
from pathlib import Path
import platform
import re
import shutil
import struct
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
CACHE = ROOT / 'tool-cache'
BUILD = ROOT / 'build'
OUT = ROOT / 'output'
KEY = ROOT / 'airow-development.p12'
GOOGLE = 'https://dl.google.com/android/repository/'
PACKAGES = {
    'Linux': ('build-tools_r35_linux.zip', '2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc'),
    'Darwin': ('build-tools_r35_macosx.zip', '93ab8ce91230e067b5add4bfa79919c52b27f072'),
    'Windows': ('build-tools_r35_windows.zip', 'af059bb67cf7786f45ee0db85e2d24985df1b4b6'),
}

def download(url, dest, digest=None, algorithm='sha1'):
    if not dest.exists():
        print('Downloading', dest.name, flush=True)
        partial = dest.with_suffix(dest.suffix + '.part')
        with urllib.request.urlopen(url, timeout=90) as response, partial.open('wb') as out:
            shutil.copyfileobj(response, out)
        partial.replace(dest)
    if digest and hashlib.new(algorithm, dest.read_bytes()).hexdigest() != digest:
        raise RuntimeError('Checksum mismatch: ' + dest.name)
    return dest

def extract(archive, destination):
    if destination.exists():
        return
    destination.mkdir(parents=True)
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            target = (destination / item.filename).resolve()
            if destination.resolve() not in target.parents and target != destination.resolve():
                raise RuntimeError('Invalid archive path')
            z.extract(item, destination)
            mode = (item.external_attr >> 16) & 0o777
            if mode:
                target.chmod(mode)

def run(*args):
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)

def legacy_resource_table(data):
    """Expand SDK resource encodings unsupported by Debian's older aapt2.

    Layouts: AOSP libs/androidfw/include/androidfw/ResourceTypes.h.
    Resource IDs, keys, configurations and values are preserved.
    """
    kind, header_size, size = struct.unpack_from('<HHI', data)
    if size != len(data) or not 8 <= header_size <= size:
        raise RuntimeError('Invalid SDK resource chunk')
    header = bytearray(data[:header_size])
    if kind in (0x0002, 0x0200):  # Resource table / package containers.
        parts = []
        old_offset = new_offset = header_size
        while old_offset < size:
            child_size = struct.unpack_from('<I', data, old_offset + 4)[0]
            if child_size < 8 or old_offset + child_size > size:
                raise RuntimeError('Invalid SDK resource child chunk')
            if kind == 0x0200:
                for field in (268, 276):  # typeStrings and keyStrings offsets.
                    if struct.unpack_from('<I', data, field)[0] == old_offset:
                        struct.pack_into('<I', header, field, new_offset)
            child = legacy_resource_table(data[old_offset:old_offset + child_size])
            parts.append(child)
            old_offset += child_size
            new_offset += len(child)
        body = b''.join(parts)
    elif kind == 0x0201:  # Resource type: expand sparse/16-bit indices and compact values.
        flags = data[9]
        count, entries_start = struct.unpack_from('<II', data, 12)
        if flags == 1:
            indices = [(index, offset * 4) for index, offset in
                       struct.iter_unpack('<HH', data[header_size:header_size + count * 4])]
            count = max((index for index, _ in indices), default=-1) + 1
        elif flags == 2:
            indices = [(index, offset * 4) for index, (offset,) in enumerate(
                struct.iter_unpack('<H', data[header_size:header_size + count * 2])) if offset != 0xffff]
        elif flags == 0:
            indices = [(index, offset) for index, (offset,) in enumerate(
                struct.iter_unpack('<I', data[header_size:header_size + count * 4])) if offset != 0xffffffff]
        else:
            raise RuntimeError(f'Unsupported SDK resource type flags: {flags}')
        offsets = [0xffffffff] * count
        entries = bytearray()
        for index, offset in indices:
            pos = entries_start + offset
            entry_size, entry_flags, value = struct.unpack_from('<HHI', data, pos)
            offsets[index] = len(entries)
            if entry_flags & 8:  # Compact simple entry: key, flags/type, value.
                entries.extend(struct.pack('<HHIHBBI', 8, entry_flags & 0xff & ~8,
                                           entry_size, 8, 0, entry_flags >> 8, value))
            else:
                value_size = (struct.unpack_from('<I', data, pos + 12)[0] * 12
                              if entry_flags & 1 else 8)
                entries.extend(data[pos:pos + entry_size + value_size])
        header[9] = 0
        struct.pack_into('<II', header, 12, count, header_size + count * 4)
        body = struct.pack(f'<{count}I', *offsets) + entries
    else:
        return data
    struct.pack_into('<I', header, 4, len(header) + len(body))
    return header + body

def resource_link_jar(android_jar):
    # Keep the downloaded SDK untouched; only aapt2 consumes this resource-only copy.
    destination = BUILD / 'android-35-resources.jar'
    with zipfile.ZipFile(android_jar) as source, zipfile.ZipFile(destination, 'w') as target:
        target.writestr('resources.arsc', legacy_resource_table(source.read('resources.arsc')))
        target.writestr('AndroidManifest.xml', source.read('AndroidManifest.xml'))
    return destination

def main():
    if not shutil.which('java') or not shutil.which('keytool'):
        raise RuntimeError('Install Java 17 or newer with keytool, then run again.')
    host = platform.system()
    if host not in PACKAGES:
        raise RuntimeError('Build on a supported Linux, macOS, or Windows computer.')
    system_native_tools = host == 'Linux' and platform.machine().lower() in ('aarch64', 'arm64')
    if system_native_tools:
        for name in ('aapt2', 'zipalign'):
            path = Path('/usr/bin') / name
            if not path.is_file() or not os.access(path, os.X_OK):
                raise RuntimeError(f'Linux ARM64 requires executable {path}. Install the system aapt2 and zipalign packages, then run again.')
    for p in [CACHE, BUILD, OUT]:
        p.mkdir(exist_ok=True)
    platform_file = download(GOOGLE + 'platform-35_r02.zip', CACHE / 'platform.zip', '0bb560a90a7a2cbd0dd8348224d518b638fe7949')
    name, digest = PACKAGES[host]
    tools_file = download(GOOGLE + name, CACHE / name, digest)
    extract(platform_file, CACHE / 'platform')
    extract(tools_file, CACHE / 'tools')
    android_jar = next((CACHE / 'platform').rglob('android.jar'))
    suffix = '.exe' if host == 'Windows' else ''
    tools_dir = next((CACHE / 'tools').rglob('aapt2' + suffix)).parent
    # Google's Linux native tools are x86-64; the SDK Java JARs work on ARM64 too.
    native_tools_dir = Path('/usr/bin') if system_native_tools else tools_dir
    ecj_url = 'https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar'
    checksum_file = CACHE / 'ecj.sha1'
    if not checksum_file.exists():
        download(ecj_url + '.sha1', checksum_file)
    checksum = checksum_file.read_text().strip().split()[0]
    ecj = download(ecj_url, CACHE / 'ecj-3.39.0.jar', checksum)
    for name in ['classes', 'generated', 'dex']:
        p = BUILD / name
        if p.exists():
            shutil.rmtree(p)
        p.mkdir()
    print('Compiling Android resources and Java source…', flush=True)
    aapt2 = native_tools_dir / ('aapt2' + suffix)
    link_jar = resource_link_jar(android_jar) if system_native_tools else android_jar
    run(aapt2, 'compile', '--dir', ROOT / 'res', '-o', BUILD / 'resources.zip')
    run(aapt2, 'link', '-I', link_jar, '--manifest', ROOT / 'AndroidManifest.xml', '--java', BUILD / 'generated', '-o', BUILD / 'unsigned.apk', BUILD / 'resources.zip')
    sources = sorted((ROOT / 'src').rglob('*.java')) + sorted((BUILD / 'generated').rglob('*.java'))
    run('java', '-jar', ecj, '-8', '-proc:none', '-warn:none', '-classpath', android_jar, '-d', BUILD / 'classes', *sources)
    # Invoke the official tool JARs directly for consistent Windows/macOS/Linux behavior.
    run('java', '-cp', tools_dir / 'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', android_jar, '--min-api', '29', '--output', BUILD / 'dex', *sorted((BUILD / 'classes').rglob('*.class')))
    with zipfile.ZipFile(BUILD / 'unsigned.apk', 'a', compression=zipfile.ZIP_STORED) as z:
        for dex in sorted((BUILD / 'dex').glob('*.dex')):
            z.write(dex, dex.name)
    run(native_tools_dir / ('zipalign' + suffix), '-f', '-p', '4', BUILD / 'unsigned.apk', BUILD / 'aligned.apk')
    if not KEY.exists():
        if os.environ.get('CI'):
            raise RuntimeError('Signing key missing in CI. Restore AIROW_SIGNING_P12_B64 before building.')
        print('Creating the development signing key. Keep this file for updates.', flush=True)
        run('keytool', '-genkeypair', '-keystore', KEY, '-storetype', 'PKCS12', '-storepass', 'android', '-keypass', 'android', '-alias', 'airow-dev', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=AIROW Personal Development')
    manifest = (ROOT / 'AndroidManifest.xml').read_text(encoding='utf-8')
    match = re.search(r'android:versionName="([^"]+)"', manifest)
    if not match:
        raise RuntimeError('AndroidManifest.xml is missing android:versionName')
    output = OUT / f'AIROW-Home-{match.group(1)}.apk'
    apksigner = tools_dir / 'lib/apksigner.jar'
    run('java', '-jar', apksigner, 'sign', '--ks', KEY, '--ks-key-alias', 'airow-dev', '--ks-pass', 'pass:android', '--out', output, BUILD / 'aligned.apk')
    run('java', '-jar', apksigner, 'verify', '--verbose', output)
    print('APK:', output)
    print('SHA-256:', hashlib.sha256(output.read_bytes()).hexdigest())

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Build stopped:', error, file=sys.stderr)
        sys.exit(1)
