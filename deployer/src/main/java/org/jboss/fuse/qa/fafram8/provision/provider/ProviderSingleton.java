package org.jboss.fuse.qa.fafram8.provision.provider;

import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Singleton which contains instance of provision provider.
 * @author jludvice
 *
 */
@Slf4j
public enum ProviderSingleton {
	INSTANCE(new StaticProvider());

	private String name;
	private ProvisionProvider provider;

	/**
	 * Private constructor for creating singleton instance.
	 * @param provider default instance of provision provider
	 */
	ProviderSingleton(ProvisionProvider provider) {
		setProvider(provider);
	}

	/**
	 * Getter.
	 * @return class name of current {@link ProvisionProvider}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Geter.
	 * @return reference to current {@link ProvisionProvider}
	 */
	public ProvisionProvider getProvider() {
		return provider;
	}

	/**
	 * Set provider and also update {@link FaframConstant#PROVIDER}.
	 * @param provider instance of provision provider
	 */
	public void setProvider(ProvisionProvider provider) {
		if (log != null) {
			// it can throw java.lang.ExceptionInInitializerError because of log == null when called during enum constructor
			log.info("Setting provision provider to {}", provider);
		}
		this.name = provider.getClass().getName();
		this.provider = provider;
		SystemProperty.set(FaframConstant.PROVIDER, name);
	}

	/**
	 * Check if current provision provider is {@link StaticProvider}.
	 * @return return true if current provision provider is static provider
	 */
	public boolean isStaticProvider() {
		return StaticProvider.class.getName().equals(name);
	}

	/**
	 * Check if current provision provider is {@link OpenStackProvisionProvider}.
	 * @return return true if current provision provider is OS provider
	 */
	public boolean isOpenstackProvider() {
		return OpenStackProvisionProvider.class.getName().equals(name);
	}
}
