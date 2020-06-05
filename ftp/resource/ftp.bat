SET JARFILE=C:\Users\ellio\tmp\cs549\ftp-test\ftp.jar
REM To convert forward slash to back-slash for windows paths
SET "JARFILE=%JARFILE:/=\%"
SET POLICY=C:\Users\ellio\tmp\cs549\ftp-test\client.policy
SET "POLICY=%POLICY:/=\%"
SET TESTDIR=C:\Users\ellio\tmp\cs549\ftp-test
SET "TESTDIR=%TESTDIR:/=\%"

if NOT EXIST %JARFILE% (
	echo "Missing jar file: %JARFILE%"
	echo "Please assemble the ftpclient jar file."
	exit 1
)

if NOT EXIST %POLICY% (
	pushd %TESTDIR%
	jar xf "%JARFILE%" client.policy
	popd
)

echo "Running client"
echo "java -Djava.security.policy=%POLICY% -jar %JARFILE%"
java -Djava.security.policy=%POLICY% -jar %JARFILE%