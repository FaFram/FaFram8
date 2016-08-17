package org.jboss.fuse.qa.fafram8.cluster.xml.broker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Class representing the <ports> element.
 * Created by avano on 4.8.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class PortsModel {
	@XmlElement(name = "port")
	private List<PortModel> ports;
}
