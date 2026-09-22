#!/usr/bin/env python3
"""Check status freshness and fail-closed parsing with the standard org.json JVM port."""
from pathlib import Path
import os
import sys
from build import ROOT, CACHE, BUILD, download, run

run(sys.executable, '-m', 'unittest', 'discover', '-s', 'tests', '-p', 'test_build.py')

CACHE.mkdir(exist_ok=True)
url='https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar'
digest=download(url+'.sha1',CACHE/'json.sha1').read_text().strip().split()[0]
jar=download(url,CACHE/'json-20240303.jar',digest)
dest=BUILD/'test-classes'
dest.mkdir(parents=True,exist_ok=True)
run('java','-jar',CACHE/'ecj-3.39.0.jar','-8','-proc:none','-warn:none','-classpath',jar,'-d',dest,ROOT/'src/com/airow/launcher/NflSnapshot.java',ROOT/'src/com/airow/launcher/NflBoard.java',ROOT/'tests/NflSnapshotTest.java',ROOT/'tests/NflBoardTest.java')
run('java','-cp',str(dest)+os.pathsep+str(jar),'com.airow.launcher.NflSnapshotTest')
run('java','-cp',str(dest)+os.pathsep+str(jar),'com.airow.launcher.NflBoardTest')
run('java','-jar',CACHE/'ecj-3.39.0.jar','-8','-proc:none','-warn:none','-classpath',str(jar)+os.pathsep+str(dest),'-d',dest,ROOT/'src/com/airow/launcher/HomeLayout.java',ROOT/'src/com/airow/launcher/ModelFreshness.java',ROOT/'tests/HomeLayoutTest.java',ROOT/'tests/ModelFreshnessTest.java')
run('java','-cp',str(dest)+os.pathsep+str(jar),'com.airow.launcher.HomeLayoutTest')
run('java','-cp',str(dest)+os.pathsep+str(jar),'com.airow.launcher.ModelFreshnessTest')
