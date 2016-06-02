package org.jboss.fuse.qa.fafram8.modifier;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.executor.Executor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Modifier interface.
 * Created by jludvice on 4.8.15.
 */
@EqualsAndHashCode
public abstract class Modifier {
	@Getter
	@Setter
	private Executor executor;
	@Getter
	@Setter
	private String host;

	/**
	 * This method will be called before fuse is started.
	 */
	public abstract void execute(Container container);
}
