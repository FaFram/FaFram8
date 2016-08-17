package org.jboss.fuse.qa.fafram8.cluster.xml.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import lombok.Getter;
import lombok.Setter;

/**
 * OneAttributeModel represents the XML element without value, but with one attribute only.
 * examples: <iptables path="path"/> <jdk path="path"/>
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class OneAttributeModel {
	@XmlAttribute
	private String value;
}
