"""Compatibility checks for the resource table used by ARM64 system aapt2."""
import struct
import unittest

from build import legacy_resource_table


def chunk(kind, header, body):
    return struct.pack('<HHI', kind, 8 + len(header), 8 + len(header) + len(body)) + header + body


def resource_type(flags, count, offsets, entries):
    # A minimal ResTable_config follows the type header.
    header = struct.pack('<BBHIII', 1, flags, 0, count, 24 + len(offsets), 4)
    return chunk(0x0201, header, offsets + entries)


class LegacyResourcesTest(unittest.TestCase):
    def test_offset16_compact_value_and_missing_entry(self):
        compact = struct.pack('<HHI', 42, 0x120a, 1)  # Public boolean, key 42.
        source = resource_type(2, 2, struct.pack('<HH', 0xffff, 0), compact)
        result = legacy_resource_table(source)
        self.assertEqual(result[9], 0)
        self.assertEqual(struct.unpack_from('<II', result, 12), (2, 32))
        self.assertEqual(struct.unpack_from('<II', result, 24), (0xffffffff, 0))
        self.assertEqual(result[32:], struct.pack('<HHIHBBI', 8, 2, 42, 8, 0, 0x12, 1))

    def test_sparse_indices_and_complex_maps_are_preserved(self):
        simple = struct.pack('<HHIHBBI', 8, 0, 9, 8, 0, 3, 17)
        complex_entry = struct.pack('<HHIII', 16, 3, 10, 0x01010000, 1)
        complex_entry += struct.pack('<IHBBI', 0x01010001, 8, 0, 0x10, 123)
        source = resource_type(1, 2, struct.pack('<HHHH', 2, 0, 5, 4), simple + complex_entry)
        result = legacy_resource_table(source)
        self.assertEqual(struct.unpack_from('<II', result, 12), (6, 48))
        self.assertEqual(struct.unpack_from('<6I', result, 24),
                         (0xffffffff, 0xffffffff, 0, 0xffffffff, 0xffffffff, 16))
        self.assertEqual(result[48:], simple + complex_entry)
        self.assertEqual(legacy_resource_table(result), result)

    def test_container_sizes_and_string_pool_offsets_are_updated(self):
        compact = struct.pack('<HHI', 7, 0x0308, 15)
        source_type = resource_type(2, 2, struct.pack('<HH', 0, 0xffff), compact)
        pool = chunk(1, b'', b'unchanged string pool')
        package_header = bytearray(280)
        struct.pack_into('<I', package_header, 0, 1)
        pool_offset = 288 + len(source_type)
        struct.pack_into('<I', package_header, 260, pool_offset)
        struct.pack_into('<I', package_header, 268, pool_offset + len(pool))
        package = chunk(0x0200, package_header, source_type + pool + pool)
        table = chunk(2, struct.pack('<I', 1), package)
        result = legacy_resource_table(table)
        self.assertEqual(struct.unpack_from('<I', result, 4)[0], len(result))
        package = result[12:]
        self.assertEqual(struct.unpack_from('<I', package, 4)[0], len(package))
        for field in (268, 276):
            offset = struct.unpack_from('<I', package, field)[0]
            self.assertEqual(package[offset:offset + len(pool)], pool)
        self.assertEqual(legacy_resource_table(result), result)

    def test_rejects_invalid_chunks_and_unknown_encoding(self):
        with self.assertRaises(RuntimeError):
            legacy_resource_table(struct.pack('<HHI', 2, 8, 0))
        with self.assertRaises(RuntimeError):
            legacy_resource_table(chunk(2, b'', struct.pack('<HHI', 1, 8, 0)))
        with self.assertRaises(RuntimeError):
            legacy_resource_table(resource_type(4, 0, b'', b''))


if __name__ == '__main__':
    unittest.main()
