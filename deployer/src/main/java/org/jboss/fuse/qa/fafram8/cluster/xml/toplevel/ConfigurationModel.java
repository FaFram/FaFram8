package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import org.jboss.fuse.qa.fafram8.cluster.xml.util.OneAttributeModel;
import org.jboss.fuse.qa.fafram8.property.FaframConstant;
import org.jboss.fuse.qa.fafram8.property.FaframProvider;
import org.jboss.fuse.qa.fafram8.property.SystemProperty;
import org.jboss.fuse.qa.fafram8.resource.Fafram;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ConfigurationModel represents the XML element <configuration>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@Getter
@Setter
public class ConfigurationModel {
	@XmlElement
	private String provider;

	@XmlElement
	private String hostUser;

	@XmlElement
	private String hostPassword;

	@XmlElement
	private String hostPort;

	@XmlElement
	private String fuseUser;

	@XmlElement
	private String fusePassword;

	@XmlElement
	private String fuseZip;

	@XmlElement
	private String startWaitTime;

	@XmlElement
	private String stopWaitTime;

	@XmlElement
	private String provisionWaitTime;

	@XmlElement
	private boolean skipBrokerWait;

	@XmlElement
	private boolean offline;

	@XmlElement
	private OneAttributeModel iptables;

	@XmlElement
	private OneAttributeModel jdk;

	@XmlElement
	private boolean withoutDefaultUser;

	/**
	 * Sets the system properties according to the parsed values.
	 * @param fafram fafram instance
	 */
	public void applyConfiguration(Fafram fafram) {
		if (provider != null) {
			if (provider.toLowerCase().contains("static")) {
				fafram.provider(FaframProvider.valueOf("STATIC"));
			} else if (provider.toLowerCase().contains("openstack")) {
				fafram.provider(FaframProvider.valueOf("OPENSTACK"));
			}
		}
		if (hostUser != null) {
			SystemProperty.set(FaframConstant.HOST_USER, hostUser);
		}
		if (hostPassword != null) {
			SystemProperty.set(FaframConstant.HOST_PASSWORD, hostPassword);
		}
		if (hostPort != null) {
			SystemProperty.set(FaframConstant.HOST_PORT, hostPort);
		}
		if (fuseUser != null) {
			SystemProperty.set(FaframConstant.FUSE_USER, fuseUser);
		}
		if (fusePassword != null) {
			SystemProperty.set(FaframConstant.FUSE_PASSWORD, fusePassword);
		}
		if (fuseZip != null) {
			SystemProperty.set(FaframConstant.FUSE_ZIP, fuseZip);
		}
		if (startWaitTime != null) {
			SystemProperty.set(FaframConstant.START_WAIT_TIME, startWaitTime);
		}
		if (stopWaitTime != null) {
			SystemProperty.set(FaframConstant.STOP_WAIT_TIME, stopWaitTime);
		}
		if (provisionWaitTime != null) {
			SystemProperty.set(FaframConstant.PROVISION_WAIT_TIME, provisionWaitTime);
		}
		if (skipBrokerWait) {
			SystemProperty.set(FaframConstant.SKIP_BROKER_WAIT, "true");
		}
		if (offline) {
			SystemProperty.set(FaframConstant.OFFLINE, "true");
		}
		if (iptables != null) {
			SystemProperty.set(FaframConstant.IPTABLES_CONF_FILE_PATH, iptables.getValue());
		}
		if (jdk != null) {
			SystemProperty.set(FaframConstant.JAVA_HOME, jdk.getValue());
		}
		if (withoutDefaultUser) {
			SystemProperty.set(FaframConstant.SKIP_DEFAULT_USER, "true");
		}
	}
}
