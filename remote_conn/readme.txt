Compile RMI Stubs
rmic -d bin -classpath bin;\work\idmunit\idmunit-core\bin com.trivir.idmunit.connector.RemoteConnectorImpl 

Start the RMI Registry
java -classpath %CLASSPATH%;./bin;c:/work/IdMUnit/idmunit-core/bin;c:/workspace/ace_conn/bin sun.rmi.registry.RegistryImpl 
