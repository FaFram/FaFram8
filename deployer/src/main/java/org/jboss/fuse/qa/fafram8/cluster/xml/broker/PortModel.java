package org.jboss.fuse.qa.fafram8.cluster.xml.broker;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Created by avano on 4.8.16.
 */
public class PortModel {
	@XmlAttribute
	private String protocol;

	@XmlAttribute
	private String port;
}
