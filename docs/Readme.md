# FaFram documentation

## SSHClient

SSHClient is based on the JSch library. Currently there are two SSHClients - _NodeSSHClient_ and _FuseSSHClient_ both implementing the
abstract _SSHClient_ class.

The abstract SSHClient class has a set of common methods that both clients use and one abstract method:


	public abstract String executeCommand(String command, boolean supressLog) throws KarafSessionDownException,
			SSHClientException


that both clients implement in a different way.

The difference is in the exception handling and more details about this can be found in the respective implementations of the abstract method.

## Deployer

Deployer represents the main functionality of FaFram8 framework and it is using the Fuse- and NodeSSH- clients. The framework can be ran from
the test in two ways:

First is to use the JUnit rule:


	@Rule
	public Fafram fafram = new Fafram();


Second is to trigger the start manually from the test (and tearDown from @After/@AfterClass)


	public class ExampleTest {
		private Fafram fafram;

		@Before
		public void init() {
			fafram = new Fafram();
			fafram.setup();
		}

		@Test
		public void dummyTest() {
		}

		@After
		public void tearDown() {
			fafram.tearDown();
		}
	}

There are two basic deployment structures: _local deployment_ and _remote deployment_.

### Local deployment

Local deployment can be used to run a single Fuse/A-MQ instance on your localhost. It gets the distribution zip file from your maven local
repository (if no additional properties are specified). Then it does a list of steps necessary to set up the distribution (unzip, modify,
run) and the whole workflow looks like this:

	Get the zip file
	Unzip into the target folder
	Modify the container
	Start the container
	Create fabric if specified
	Patch the distribution if specified
	<your test>
	Stop the container
	Delete the container directory if it's desired

### Remote deployment

Remote deployment can be used for running Fuse/A-MQ instance on remote host. Its functionality is almost similar to local deployment with some small differences. The first one is that it doesn't get distribution zip file from maven local repository and it is required to specify **fuse.zip** property. This property should be either path to distribution zip present on the remote host or URL for downloading the distribution zip from the net. Last difference is that the distribution zip is unzipped to _fafram/${FUSE_NAME}_ folder in the user's home directory on specified machine. The whole workflow for remote deployment looks like this:

	Get zip file from specified fuse.zip property(file or download)
	Unzip into fafram folder that is created on the remote machine
	Modify the container
	Start the container
	Create fabric if specified
	Patch the distribution if specified
	Create specified containers
	<your test>
	Stop the container
	Delete the all containers if it's desired

If you want tell FaFram8 where to work its magic, you can specify working directory by setting system property **fafram.working.directory**. For example `-Dfafram.working.directory=/path/to/folder` will create default fafram folder in folder /path/to/folder/fafram and that is where Fuse distribution will be unzipped. This property will be also used when creating SSH containers. That means that SSH container will be created to folder `/path/to/folder/containers/${container.name}`. FaFram8 will not try to create this folder so it required that this folder exists on all used machines.

#### Providers
Remote deployment has one more special feature that is a concept of providers. Providers tell FaFram how the remote deployment should be handled. For now FaFram supports 2 types of remote deployment(a.k.a two different providers):
* Static deployment with provided IP address(**StaticProvider**)
* Openstack deployment that spawns machines on Openstack(**OpenstackProvider**)


#### Static provider
Static provider is the default provider for FaFram and its remote deployment. It is used when user already have running machines for deployment with static IP addresses or hostnames. In this case you just specify IP addresses to the machines. There is also possibility to explicitly define static provider with _provider(FaframProvider.STATIC)_ but it is not required by default.


This is basic example of static provider or default behavior of Fafram for spawning 1 root container on host "1.2.3.4" and the SSH container on address "5.6.7.8".
```
public Fafram fafram = new Fafram().fuseZip("http://path/to/fuse.zip").containers(
			RootContainer.builder().name("myroot").withFabric().node(
					host("1.2.3.4")
							.port(22)
							.username("myuser")
							.password("mypassword")
							.build()
			).build(),
			SshContainer.builder().name("ssh").node("5.6.7.8", 22, "nextUser", "nextPassword")
			.parentName("myroot").build()
	);
```

#### Openstack provider
Openstack provider is on the other hand used for dynamic provisioning of machines on Openstack. This is not the default behavior of FaFram8 so user needs to specify it using the _provider()_ method on _Fafram_ class. FaFram provides static enum _FaframProvider.OPENSTACK_ for defining that Openstack provider should be used. Afterwards for all containers without defined node there will be an openstack instance spawned and the IP assigned into the container object.

Full example of using the dynamic provisioning of machines using the Openstack provider.
```
private Container root = RootContainer.builder().name("offline-cluster-root").withFabric().build();
private Container sshContainer = SshContainer.builder().name("ssh1-offline").parent(root).profiles("gateway-mq").build();
private Container sshContainer2 = SshContainer.builder().name("ssh2-offline").parent(root).profiles("feature-camel", "complex-create").build();
private static Container sshChild = ChildContainer.builder().name("child-ssh-offline1").parent(sshContainer).build();

@Rule
public static Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, sshContainer, sshContainer2, sshChild);
```
This example spawns 3 machines on the Openstack and creates:
* root container
* ssh container with name ssh1-offline
* ssh container with ssh2-offline
* child container on machine of ssh container with name ssh1-offline

#### Uploading bundles to remote Fabric
Fafram8 supports uploading bundles from local machine to remote deployment of Fuse. This functionality is provided by _MavenPomInvoker_ class which invokes Maven project and uploads built bundle to fabric maven proxy on remote root container.

The project for uploading must have specific configuration in its _pom.xml_:

 ```
<distributionManagement>
      <repository>
        <id>fuse-maven-proxy</id>
        <name>fuse-maven-proxy</name>
        <url>${mvn.proxy.upload.url}</url>
     </repository>
</distributionManagement>
 ```

When the project is configured correctly then you need to add path to _pom.xml_ using the _bundles(String... commands)_ method on _Fafram_ class. Also it is possible to specify multiple paths to different projects.

 ```
@Rule
public Fafram fafram = new Fafram().withFabric().bundle("src/test/resources/blank-project/pom.xml", "test/project/pom.xml");
 ```

Bundles are uploaded to fabric maven proxy before execution of commands specified by _command(String... commands)_ method. This can be leverage for example when editing profiles and adding your custom bundles.

It is also possible to upload bundle to maven proxy right in the test after Fafram8 created its environment. Only restriction is that you need to know to which container of the cluster you can upload the bundle meaning you need to know on which container is maven proxy is located (in most use cases it will be on the root container).

```
@Rule
public Fafram fafram = new Fafram().withFabric().provider(FaframProvider.OPENSTACK).containers(RootContainer.builder().defaultRoot().withFabric().build());

@Test
public void testUploadBundleFromTest() throws Exception {
	((RootContainer) fafram.getContainer("root")).uploadBundles("src/test/resources/blank-project/pom.xml");
	fafram.executeCommand("osgi:install mvn:org.jboss.fuse.qa.test/useless-artifact/1.0");
}
```

#### Connecting to already running Fuse instance
Using Fafram8 you are able to connect to running Fuse instance and work with it. For this feature it is required to create dummy root container and specify IP address in its node and add _onlyConnect()_ method if you want to connect to container using 8101 port which is the default Fuse SSH port or _onlyConnect(int port)_ method when connecting to container running on different SSH port.

```
@Rule
Fafram fafram = new Fafram().containers(RootContainer.builder().name("dummy-container").node("1.1.1.1").onlyConnect().build())

@Rule
Fafram fafram = new Fafram().containers(RootContainer.builder().name("dummy-container").node("1.1.1.1").onlyConnect(8103).build())
```

### Loading iptables and offline mode
**This is experimental feature and it requires that you know what are you doing and also that you are prepared to bear the consequences!**

FaFram provides feature for modifying iptables on remote machines using the _loadIPtablesConfigurationFile("local/path/to/file")_. This feature works for both the Static provider and Openstack provider. It requires that the ssh user used for connecting to machines is member of sudoers and _sudo_ command can be executed without password. FaFram will copy the specified file to all machines and set the _iptables_ on each machine to reflect configuration specified in specified configuration file. Meaning that the whole environment will get to state defined by the user.

If Static provider is used then old _iptables_ configuration is saved to temp file with name **ipTablesSaved** on each machine. This configuration is always restored after test is done or if there was problem with the test. When using Openstack provider then iptables are never cleaned.

FaFram also provide special option _offline()_ that works only if iptables configuration file with name **iptables-no-internet** is present on all nodes in user home directory. If you are using the Openstack provider then this file is present in all ecervena snapshosts by default. Using offline flag with dynamic provisioning with Fuse-QA Openstack is the most stable and the most supported use case for this feature.

For basic examples and usage check [OfflineTest](../deployer/src/test/java/org/jboss/fuse/qa/fafram8/test/remote/RemoteTurnOffInternetTest.java) and [CustomIPTablesTest](../deployer/src/test/java/org/jboss/fuse/qa/fafram8/test/remote/RemoteSettingIPtablesTest.java).

### System properties
The workflow or properties can be modified using system properties. Full list of system properties is

* host - Host IP address
* host.port - Host port
* host.user - Host SSH login
* host.password - Host SSH password
* fuse.user - Fuse user
* fuse.password - Fuse password
* fuse.group - Fuse artifact maven group id
* fuse.id - Fuse artifact maven artifact id
* fuse.version - Fuse version, for example 6.2.0.redhat-133
* fuse.zip - Fuse zip location
* start.wait.time - Karaf start wait time
* stop.wait.time - Karaf stop wait time
* provision.wait.time - Fabric provision wait time
* patch.wait.time - Patch install wait time
* keepFolder - Flag if the folder should be kept after the execution
* fafram.folder - Name of the working folder on remote host
* patch - Patch location
* fafram.working.directory - Special working directory on remote machine
* fafram.patch.dir - Patch directory location
* fafram.skip.default.user - Skip default user add
* fafram.patch.standalone - Patch standalone - useful only together with .withFabric() method
* fafram.skip.default.jvm.opts - Skip adding default JVM opts (xms=768M, xmx=1536M, permMem=768M, maxPermMem=1536M)
* fafram.suppress.start - Testing purposes only - do not start fuse for the tests that do not need it
* fafram.archive.target - Target dir where the archived artifacts will be stored
* fafram.archive.pattern - Archive pattern equal to the jenkins archive pattern
* fafram.base.dir - Base dir of the project - on localhost it's "" and on jenkins it's System.getenv("WORKSPACE")
* keep.os.resources - If "true" OpenStackProvisionManager will not release OS nodes after test
* broker.start.wait.time - Broker start wait time
* fafram.skip.broker.wait - Skips the initial waiting for the broker
* jira.url - JIRA url
* jira.user - JIRA user
* jira.password - JIRA password
* openstack.url - openstack URL
* openstack.tenant - openstack tenant
* openstack.user - openstack user
* openstack.password - openstack password
* openstack.image - openstack image uuid
* openstack.flavor - openstack flavor
* openstack.keypair - openstack keypair
* openstack.addressType - openstack address type
* fafram.offline - Flag for offline environment
* iptables.conf.file.path - Path to iptables file that should be applied before starting fuse
* fafram.default.root.name - Default container name (defaults to root)
* keepContainers - Flag if the containers should be kept running
* keepAll - Combination of `keepContainers` and `keep.os.resources`
* additional.commands - Semicolon separated list of commands that should be executed (useful for small hotfixy specific for builds with problems etc.)
* use.default.repositores - Flag if the default fabric repositories should be used
* command.retry.timeout - Retry timeout in seconds when the command response contains "not found"
* no.threads -

### Patches

Patcher class is used to prepare the patch zips to be installed. Currently the **patch** property accepts four ways how you can define the patch:

* -Dpatch=r2 - scans the <fafram.patch.dir> recursively for the patch zip name that contains the string "r2" and returns the first occurance.
	The patches are (hopefully) always sorted lexicographically, so r2 patch zip should be found sooner than r2p4 for example. It is possible
	to use the string "r2,r2p4" where both patch zips will be returned and installed. At the same time it checks if the patch version matches
	the current version set by the fuse.version property.
* -Dpatch=latest - scans the <fafram.patch.dir>/latest folder and returns all the files in the folder. Again, they should be sorted so the right
 order should be preserved.
* -Dpatch=file:///home/file.zip - returns the zip file location
* -Dpatch=http://www.example.com/file.zip - returns the zip file location

### Container zip location

By default the FaFram8 framework gets the container zip from the local maven repository. First of all,
it gets the maven local repository location using the maven invoker. For the maven invoker you need to have a maven binary in your **PATH**
environment variable, or the **M2_HOME** system or environment property set. The FaFram8 framework then constructs the absolute path to the
container zip file using the **fuse.group**, **fuse.id** and **fuse.version** properties.

This default behavior can be overriden with setting the **fuse.zip** property in this way:

* -Dfuse.zip=file:///home/file.zip - unzips the file directly from this location
* -Dfuse.zip=http://www.example.com/file.zip - downloads the file to the machine and then unzips it

### Fafram8 JUnit Test runner

You can annotate your JUnit test class with `@RunWith(FaframTestRunner.class)` to be able to use the `@Jira("xxx")` annotation for your test.
This annotation will check the jira status and can be used to skip the test if the issue is not fixed. The test will be run if the jira is in
"Resolved" or "Closed" state, otherwise the test will be skipped. The test runner also prints the name of the current test method before the
actual test execution.

### External configuration

OpenStack and JIRA configuration is done via the _fafram.properties_ file. Sample configuration file can be found in
_deployer/src/main/resources/fafram.properties_ where all supported values are defined. If you will want to change these values in your tests,
you can place your own fafram.properties file in your test resources (you don't necessary need to overwrite all the properties). This file is
then merged with our properties file (with your changes on top of the default values) and the final configuration is created and used.

### Wait for anything

If you need to wait for some url to return HTTP Status 200, any specific result of ssh command or whatever else,
you can use method `fafram.waitFor(Callable<Response<T>> callable, long secondsTimeout)` to do so.
You just write class (may be anonymous) implementing `Callable<Response<T>>` where `T` is data you expect - eg. `HttpEntity` or `String`.

Fafram will execute `call` method of your class every 3 seconds until you
`return Response.success(T data)` or you `return Response.timeout()` and time goes out.


### Tests

Tests can be found in the respective directory in the module directories. Tests are skipped by default and this behavior can be overriden
using the **skip.test.fafram** property.

Example usage:

	cd deployer; mvn clean install -Dskip.test.fafram=false -Dtest=HostValidatorTest

### Building Maven projects

Fafram8 provides support for building custom Maven project with custom goals and properties. This feature is useful e.g if you want to build a quickstart project can be later deployed to running Fuse via _osgi:install mvn:group/artifact/1.0_. Defined projects are built before unzipping and starting Fuse. That means you can also defined commands for adding built project to Fuse right on the Fafram object.

 There is possibility to define projects for maven execution right on the Fafram object with method _buildBundle()_ or use static method _MavenPomInvoker.buildMvnProject()_ with different paramaters.

 ```
 Map<String, String> properties = new HashMap<>();
 properties.put("custom.property", "faframIsGreat");

 public Fafram fafram = new Fafram().buildBundle("src/test/resources/blank-project/pom.xml", "clean", "install").buildBundle("path/test/pom.xml", properties, "clean", "deploy").commands(osgi:install -s mvn:org.jboss.fuse.qa.test/useless-artifact/1.0);
 ```

  ```
 Map<String, String> properties = new HashMap<>();
 properties.put("custom.property", "faframIsGreat");
 List<String> goals = new ArrayList<>();
 goals.add("install");

 MavenPomInvoker.buildMvnProject("/home/user/path/pom.xml", properties, "clean", "package");

 MavenPomInvoker.buildMvnProject(new MavenProject("/home/user/path/pom.xml", properties, goals);

 MavenPomInvoker.buildMvnProject(new MavenProject("/home/user/path/pom.xml", properties, "clean", "test);
 ```

## FaFram8 example usage

The only necessary thing to include in your pom.xml file is the dependency for the framework. However, if you want your test to download the
container automatically and use it, you need to include this in your pom.xml file aswell.

	<?xml version="1.0" encoding="UTF-8"?>
	<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
		<modelVersion>4.0.0</modelVersion>

		<groupId>com.test</groupId>
		<artifactId>test</artifactId>
		<version>1.0-SNAPSHOT</version>

		<properties>
			<fuse.group>org.jboss.fuse</fuse.group>
			<fuse.id>jboss-fuse-full</fuse.id>
			<fuse.version>6.2.0.redhat-133</fuse.version>
        </properties>

		<repositories>
				<!-- Fuse QE repositories (Fafram8 releases) -->
				<repository>
					<id>fuse-qe-repo</id>
					<url>http://fusewin.tpb.lab.eng.brq.redhat.com:8081/nexus/content/repositories/fuse-qe-repo/</url>
					<snapshots>
						<enabled>true</enabled>
					</snapshots>
					<releases>
						<enabled>true</enabled>
					</releases>
				</repository>
		</repositories>

		<dependencies>
			<dependency>
				<groupId>org.jboss.fuse.qa</groupId>
				<artifactId>fafram8</artifactId>
				<version>fafram version</version>
				<scope>test</scope>
			</dependency>
			<dependency>
				<groupId>junit</groupId>
				<artifactId>junit</artifactId>
				<version>4.12</version>
				<scope>test</scope>
			</dependency>
			<dependency>
				<groupId>org.jboss.fuse</groupId>
				<artifactId>jboss-fuse-full</artifactId>
				<type>zip</type>
				<version>${fuse.version}</version>
				<!-- We want just the zip without all other transitive dependencies -->
				<exclusions>
					<exclusion>
						<groupId>*</groupId>
						<artifactId>*</artifactId>
					</exclusion>
				</exclusions>
			</dependency>
		</dependencies>

		<build>
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-surefire-plugin</artifactId>
					<version>2.18.1</version>
					<configuration>
						<skipTests>false</skipTests>
						<systemPropertyVariables>
							<fuse.group>org.jboss.fuse</fuse.group>
							<fuse.id>${fuse.id}</fuse.id>
							<fuse.version>${fuse.version}</fuse.version>
						</systemPropertyVariables>
					</configuration>
				</plugin>
			</plugins>
		</build>

	</project>

In this example, we added the dependency for jboss-fuse-full zip file, so that the maven will download the zip file into our local repository before
the test execution. Using the maven-surefire-plugin we pass the **fuse.group**, **fuse.id**, **fuse.version** properties into the test, that are
later used to construct the full path to your local repository artifact path. Using these properties you can easily switch between versions
and even between the Fuse and A-MQ distributions.

There is no need to use this approach, because you can still use some other (previously downloaded) zip file and pass it to FaFram using the **fuse.zip** property.

You can find more examples in [Examples.md](Examples.md) file.

## Checkstyle

We are using checkstyle! If you want to control your code, use the **checkstyle** system property.

	mvn clean install -Dcheckstyle

For further info about contributing see _Readme.md_ in the root directory of the project.
