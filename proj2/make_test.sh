#!/bin/bash

#d="gitlet/"
#javac ${d}Main.java ${d}CommitTree.java ${d}Utils.java ${d}GitletException.java;

# Run me with: bash make_test.sh

path="gitlet"
files=""

for f in ${path}/*.java; do 
	if [ $f != ${path}/UnitTest.java ]; then 
		echo "Compiling $f";
		files="$files $f"
	fi; 
done

javac $files

testdir="test-gitlet"
rm -r $testdir
mkdir $testdir
mkdir $testdir/gitlet
cp gitlet/*.class ./$testdir/gitlet
echo "Creating file called test.txt with text 'version 1'"
touch ./$testdir/test.txt
echo "version 1" >> ./$testdir/test.txt
cd $testdir
echo "java gitlet.Main init"
java gitlet.Main init
echo "java gitlet.Main add test.txt"
java gitlet.Main add "test.txt"
echo "gitlet.Main log"
java gitlet.Main log
echo "gitlet.Main commit 'test commit'"
java gitlet.Main commit "test commit"
echo "ls .gitlet/stage"
ls .gitlet/stage
echo "ls .gitlet/commits"
ls .gitlet/commits
echo "java gitlet.Main log"
java gitlet.Main log
echo "java gitlet.Main branch testBranch"
java gitlet.Main branch testBranch
echo "java gitlet.Main status"
java gitlet.Main status

#echo "======================="
#echo "TESTING BRANCH CHECKOUT"
#echo "======================="
#java gitlet.Main checkout testBranch
#touch text2.txt
#java gitlet.Main add text2.txt
#java gitlet.Main commit "Added text2.txt"
#echo "Before:"
#ls
#echo "After:"
#java gitlet.Main checkout master
#ls

# ======================================
echo "=============="
echo "TESTING REBASE"
echo "=============="

echo "m" >> master.txt
java gitlet.Main add master.txt
java gitlet.Main commit "Added master.txt"
java gitlet.Main checkout testBranch
echo "m" >> testBranch.txt
java gitlet.Main add testBranch.txt
java gitlet.Main commit "Added testBranch.txt"
java gitlet.Main log
java gitlet.Main rebase master
java gitlet.Main checkout master
java gitlet.Main log

echo "DONE"
cd ..
