package org.jboss.fuse.qa.fafram8.cluster.xml.util;

import org.jboss.fuse.qa.fafram8.cluster.node.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import lombok.Getter;
import lombok.Setter;

/**
 * NodeModel class represents XML configuration to object model mapping.
 * Holds information parsed from <node> tag.
 * <p/>
 * Created by ecervena on 1/13/16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class NodeModel {
	@XmlAttribute
	private String host;

	@XmlAttribute
	private int port;

	@XmlAttribute
	private String user;

	@XmlAttribute
	private String password;

	/**
	 * Creates a node object from the specified attributes.
	 * @return node object
	 */
	public Node createNode() {
		return Node.builder().host(host)
				.port(port)
				.username(user)
				.password(password).build();
	}
}
