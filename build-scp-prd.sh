mvn -T 1.5C -Dmaven.test.skip=true clean package && assert_new exotic-standalone/target/exotic-standalone-1.11.4-SNAPSHOT.jar && scp exotic-standalone/target/exotic-standalone-1.11.4-SNAPSHOT.jar tc-gz-1:~/exotic/ && say 'scp done'

if [[ $? != 0 ]];
	say 'failed'
fi
