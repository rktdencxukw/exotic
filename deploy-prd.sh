
scp exotic-standalone/target/exotic-standalone-1.11.4-SNAPSHOT.jar tc-gz-1:~/exotic/ && say 'done'

if [[ $? != 0 ]]; then
	say 'failed'
fi
