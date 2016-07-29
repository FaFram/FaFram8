package org.jboss.fuse.qa.fafram8.cluster.xml.broker;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Class representing the <pids> element.
 * Created by avano on 28.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class PidsModel {
	@XmlElement(name = "pid")
	private List<PidModel> pids;
}
