package org.jboss.fuse.qa.fafram8.openstack.provision;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.openstack.OSFactory;

import java.util.Arrays;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread worker class. OpenStack client is created with shared session from OpenStackClient. Worker purpose
 * is spawn one single server per thread, wait for "active" status.
 * <p/>
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class ServerInvoker implements Callable {

	private static final int BOOT_TIMEOUT = 300000;

	//Name of the node
	private String nodeName;

	private OpenStackClient client;

	/**
	 * Constructor for thread worker.
	 *
	 * @param nodeName name of the node
	 * @param client OpenStack client to work with
	 */
	public ServerInvoker(String nodeName, OpenStackClient client) {
		this.nodeName = nodeName;
		this.client = client;
	}

	/**
	 * Method executed in thread.
	 *
	 * @return created server
	 */
	@Override
	public Server call() {
		log.info("Creating server inside thread for container: " + nodeName);
		final OSClient os = OSFactory.clientFromAccess(client.getOsClient().getAccess());
		final ServerCreate serverCreate = os
				.compute()
				.servers()
				.serverBuilder()
				.image(client.getImage())
				.name(client.getNamePrefix() + "-" + nodeName)
				.flavor(client.getFlavor())
				.keypairName(client.getKeypair())
				.networks(Arrays.asList(client.getNetworks().split(",")))
				.build();
		return os.compute().servers().bootAndWaitActive(serverCreate, BOOT_TIMEOUT);
	}
}
