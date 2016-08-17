package org.jboss.fuse.qa.fafram8.cluster.xml.toplevel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * BundlesModel represents the XML element <bundles>.
 * Created by avano on 26.7.16.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class BundlesModel {
	@XmlElement(name = "bundle")
	private List<String> bundles;
}
