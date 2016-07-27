package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * EnsembleModel represents the XML element <ensemble>.
 * Created by avano on 26.7.16.
 */
@XmlRootElement(name = "ensemble")
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@Getter
@Setter
public class EnsembleModel {
	@XmlValue
	private String ensemble;
}
