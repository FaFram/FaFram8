package org.jboss.fuse.qa.fafram8.test.remote;

import static org.junit.Assert.assertTrue;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.cluster.container.SshContainer;
import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;
import org.jboss.fuse.qa.fafram8.exceptions.VerifyFalseException;
import org.jboss.fuse.qa.fafram8.executor.WindowsExecutor;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.provision.provider.OpenStackProvisionProvider;
import org.jboss.fuse.qa.fafram8.resource.Fafram;
import org.jboss.fuse.qa.fafram8.test.base.FaframTestBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Tests offline environment setup in Windows containers provisioned by {@link org.jboss.fuse.qa.fafram8.provision.provider.OpenStackProvisionProvider}.
 *
 * @author jknetl
 */
@Slf4j
public class RemoteTurnOffInternetWindowsTest {

	public static final String ROOT_NAME = "windows-offline-root";

	public static final String ERROR_STATEMENT = "Failed to connect";

	private Container root = RootContainer.builder().name(ROOT_NAME).withFabric().build();
	private Container ssh = SshContainer.builder().name("windows-offline-ssh").parentName(ROOT_NAME).build();

	@Rule
	public Fafram fafram = new Fafram().provider(FaframProvider.OPENSTACK).containers(root, ssh).offline().suppressStart();

	@BeforeClass
	public static void before() {
		System.setProperty(FaframConstant.FUSE_ZIP, FaframTestBase.CURRENT_WIN_LOCAL_URL);
		System.setProperty(FaframConstant.OPENSTACK_WINDOWS, "true");
		System.setProperty(FaframConstant.OPENSTACK_WAIT_TIME, "900");
		System.setProperty(FaframConstant.HOST_USER, "hudson");
		System.setProperty(FaframConstant.HOST_PASSWORD, "redhat");
	}

	@AfterClass
	public static void clean() {
		System.clearProperty(FaframConstant.FUSE_ZIP);
		OpenStackProvisionProvider.getInstance().getClient().setFlavor(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_FLAVOR));
		OpenStackProvisionProvider.getInstance().getClient().setImage(SystemProperty.getExternalProperty(FaframConstant.OPENSTACK_IMAGE));
		System.clearProperty(FaframConstant.FUSE_ZIP);
		System.clearProperty(FaframConstant.HOST_USER);
		System.clearProperty(FaframConstant.HOST_PASSWORD);
		System.clearProperty(FaframConstant.OPENSTACK_WAIT_TIME);
		System.clearProperty(FaframConstant.OPENSTACK_WINDOWS);
	}

	@Test
	public void testInternet() throws VerifyFalseException, SSHClientException, KarafSessionDownException, InterruptedException {
		final String testInternetCommand = "curl -vs www.google.com 2>&1";
		final String rootResponse = root.executeNodeCommand(testInternetCommand);
		final boolean hasRootWindowsExecutor = root.getNode().getExecutor() instanceof WindowsExecutor;

		final String sshResponse = ssh.executeNodeCommand(testInternetCommand);
		final boolean hasSshWindowsExecutor = ssh.getNode().getExecutor() instanceof WindowsExecutor;

		assertTrue(rootResponse.contains(ERROR_STATEMENT));
		assertTrue(sshResponse.contains(ERROR_STATEMENT));
		assertTrue(hasRootWindowsExecutor);
		assertTrue(hasSshWindowsExecutor);
	}
}
