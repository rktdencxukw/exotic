mvn -T 1.5C -Dmaven.test.skip=true clean package; ntyw $? && assert_new exotic-standalone/target/exotic-standalone-1.11.4-SNAPSHOT.jar && scp exotic-standalone/target/exotic-standalone-1.11.4-SNAPSHOT.jar tc-gz-1:~/exotic/ && say 'done'

if [[ $? != 0 ]]; then
	say 'failed'
fi
