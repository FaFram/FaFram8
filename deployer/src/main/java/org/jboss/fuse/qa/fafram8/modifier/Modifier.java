package org.jboss.fuse.qa.fafram8.modifier;

import org.jboss.fuse.qa.fafram8.executor.Executor;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Modifier interface.
 * Created by jludvice on 4.8.15.
 */
@EqualsAndHashCode
public abstract class Modifier {
	@Getter
	private Executor executor;
	@Getter
	private String host;

	/**
	 * This method will be called before fuse is started.
	 */
	public abstract void execute();

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
