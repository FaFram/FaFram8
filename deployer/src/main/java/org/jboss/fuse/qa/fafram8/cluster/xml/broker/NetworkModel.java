package org.jboss.fuse.qa.fafram8.cluster.xml.broker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import lombok.Getter;
import lombok.Setter;

/**
 * Class representing the <network> element.
 * Created by avano on 28.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class NetworkModel {
	@XmlAttribute
	private String url;

	@XmlAttribute
	private String username;

	@XmlAttribute
	private String password;
}
