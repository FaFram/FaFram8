package org.jboss.fuse.qa.fafram8.cluster.xml.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * UsersModel represents the XML element <users>.
 * Created by avano on 26.7.16.
 */
@XmlRootElement(name = "users")
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@Getter
@Setter
public class UsersModel {
	@XmlElement(name = "user")
	private List<UserModel> users;
}
