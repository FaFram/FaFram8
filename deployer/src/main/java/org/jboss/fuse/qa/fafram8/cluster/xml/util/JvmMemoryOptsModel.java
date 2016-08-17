package org.jboss.fuse.qa.fafram8.cluster.xml.util;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import lombok.Getter;
import lombok.Setter;

/**
 * JvmMemoryOptsModel represents the <jvmMemoryOpts> tag.
 * example: <jvmMemoryOpts xms="512M" xmx="1024M" permMem="512M" maxPermMem="1024M"/>
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class JvmMemoryOptsModel {
	@XmlAttribute
	private String xms;

	@XmlAttribute
	private String xmx;

	@XmlAttribute
	private String permMem;

	@XmlAttribute
	private String maxPermMem;
}
