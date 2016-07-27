package org.jboss.fuse.qa.fafram8.cluster.xml.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import lombok.Getter;
import lombok.Setter;

/**
 * UserModel represents the XML element <user>.
 * example: <user name="user" password="pw" roles="Administrator"/>
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class UserModel {
	@XmlAttribute
	private String name;

	@XmlAttribute
	private String password;

	@XmlAttribute
	private String roles;
}
