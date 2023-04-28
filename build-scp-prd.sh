mvn  -Dmaven.test.skip=true clean package && assert_new exotic-standalone/target/exotic-standalone-1.11.1001-SNAPSHOT.jar && scp exotic-standalone/target/exotic-standalone-1.11.1001-SNAPSHOT.jar tc-gz-1:~/exotic/ ; nw
exit 0

if [[ $? != 0 ]]; then
	say 'failed'
fi

#t
