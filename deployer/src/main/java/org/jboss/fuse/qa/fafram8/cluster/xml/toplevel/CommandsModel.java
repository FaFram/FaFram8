package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * CommandsModel represents the XML element <commands>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@ToString
@Getter
@Setter
public class CommandsModel {
	@XmlElement(name = "command")
	private List<String> commands;
}
