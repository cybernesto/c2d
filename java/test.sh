#!/bin/bash

set -e

PATH=bin:$PATH
BIN=../gameserverclient
ADDR=800
SUM=3226e0aa8f35ee23a9de9b8f05abf688

rm -f ${BIN}.dsk
echo
echo "Testing java c2d..."
echo
echo "c2d ${BIN},${ADDR} ${BIN}.dsk"
java -jar C2d.jar ${BIN},${ADDR} ${BIN}.dsk 2>&1 | sed 's/^/    /'
CHECK=$(md5 ${BIN}.dsk | awk '{print $4}')
if [ "$CHECK" = "$SUM" ]
then
	echo PASSED
else
	echo "FAILED $CHECK != $SUM (expected)"
	exit 1
fi

SUM=56a52e40a2351ff39669efa3fbdd0f19

rm -f ${BIN}.dsk
echo
echo "Testing OS/X c2d textpage..."
echo
echo "text2page <${BIN}.text | page2text | text2page >${BIN}.textpage"
text2page <${BIN}.text | page2text | text2page >${BIN}.textpage
echo "c2d -t ${BIN}.textpage ${BIN},${ADDR} ${BIN}.dsk"
java -jar C2d.jar -t ${BIN}.textpage ${BIN},${ADDR} ${BIN}.dsk 2>&1 | sed 's/^/    /'
CHECK=$(md5 ${BIN}.dsk | awk '{print $4}')
if [ "$CHECK" = "$SUM" ]
then
	echo PASSED
else
	echo "FAILED $CHECK != $SUM (expected)"
	exit 1
fi

BIN=../gameserverclient
MON=../gameserverclient.mon
ADDR=800
SUM=3226e0aa8f35ee23a9de9b8f05abf688

rm -f ${BIN}.dsk
echo
echo "Testing OS/X c2d..."
echo
echo "c2d ${MON} ${BIN}.dsk"
java -jar C2d.jar ${MON} ${BIN}.dsk 2>&1 | sed 's/^/    /'
CHECK=$(md5 ${BIN}.dsk | awk '{print $4}')
if [ "$CHECK" = "$SUM" ]
then
	echo PASSED
else
	echo "FAILED $CHECK != $SUM (expected)"
	exit 1
fi

SUM=56a52e40a2351ff39669efa3fbdd0f19

rm -f ${BIN}.dsk
echo
echo "Testing OS/X c2d textpage..."
echo
echo "text2page <${BIN}.text >${BIN}.textpage"
text2page <${BIN}.text >${BIN}.textpage
echo "c2d -t ${BIN}.textpage ${MON} ${BIN}.dsk"
java -jar C2d.jar -t ${BIN}.textpage ${MON} ${BIN}.dsk 2>&1 | sed 's/^/    /'
CHECK=$(md5 ${BIN}.dsk | awk '{print $4}')
if [ "$CHECK" = "$SUM" ]
then
	echo PASSED
else
	echo "FAILED $CHECK != $SUM (expected)"
	exit 1
fi

echo
