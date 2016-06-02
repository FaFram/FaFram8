package org.jboss.fuse.qa.fafram8.modifier;

import org.jboss.fuse.qa.fafram8.cluster.container.Container;
import org.jboss.fuse.qa.fafram8.cluster.container.RootContainer;
import org.jboss.fuse.qa.fafram8.exception.FaframException;
import org.jboss.fuse.qa.fafram8.executor.Executor;
import org.jboss.fuse.qa.fafram8.manager.ContainerManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Modifier Executor class. This class manages the (post-)modifiers.
 * Created by avano on 5.10.15.
 */
@Slf4j
public class ModifierExecutor {
	private static ModifierExecutor instance = new ModifierExecutor();
	private static Set<Modifier> modifiers = new LinkedHashSet<>();
	private static Set<Modifier> postModifiers = new LinkedHashSet<>();
	private static Set<Modifier> customModifiers = new LinkedHashSet<>();

	/**
	 * Constructor.
	 */
	protected ModifierExecutor() {
	}

	/**
	 * Gets the instance.
	 *
	 * @return instance
	 */
	public static ModifierExecutor getInstance() {
		return instance;
	}

	/**
	 * Adds the modifier into the modifier list.
	 *
	 * @param modifier modifiers
	 */
	public static void addModifiers(Modifier... modifier) {
		// Force the initialization
		ModifierExecutor.getInstance();

		synchronized (modifiers) {
			addModifiersToCollection(modifiers, modifier);
		}
	}

	/**
	 * Add modifiers to post modifiers.
	 *
	 * @param modifier modifiers
	 */
	public static void addPostModifiers(Modifier... modifier) {
		// Force the initialization
		ModifierExecutor.getInstance();

		addModifiersToCollection(postModifiers, modifier);
	}

	/**
	 * Adds modifiers to custom modifierse.
	 *
	 * @param modifier modifiers
	 */
	public static void addCustomModifiers(Modifier... modifier) {
		ModifierExecutor.getInstance();

		addModifiersToCollection(customModifiers, modifier);
	}

	/**
	 * Add modifiers to the collection.
	 *
	 * @param col collection
	 * @param modifier modifiers
	 */
	private static void addModifiersToCollection(Collection<Modifier> col, Modifier... modifier) {
		Collections.addAll(col, modifier);
	}

	/**
	 * Executes the modifiers before the fuse starts.
	 *
	 * @param container host to execute on
	 */
	public static void executeModifiers(Container container) {
		executeModifiers(container, null);
	}

	/**
	 * Executes the modifiers before the fuse starts.
	 *
	 * @param executor executor
	 * @param container host to execute on
	 */
	public static void executeModifiers(Container container, Executor executor) {
		executeModifiersFromCollection(container, executor, modifiers);
	}

	/**
	 * Executes the post modifiers.
	 */
	public static void executePostModifiers(Container container) {
		ModifierExecutor.getInstance();
		executePostModifiers(container, null);
	}

	/**
	 * Executes the post modifiers on remote.
	 *
	 * @param executor executor
	 */
	public static void executePostModifiers(Container container, Executor executor) {
		executeModifiersFromCollection(container, executor, postModifiers);
	}

	/**
	 * Executes the custom modifiers.
	 */
	public static void executeCustomModifiers(Container container) {
		executeCustomModifiers(container, null);
	}

	/**
	 * Executes the custom modifiers on remote.
	 *
	 * @param executor executor
	 */
	public static void executeCustomModifiers(Container container, Executor executor) {
		executeModifiersFromCollection(container, executor, customModifiers);
	}

	/**
	 * Executes the modifiers from the given collection.
	 *
	 * @param executor executor
	 * @param col collection
	 */
	private static void executeModifiersFromCollection(Container container, Executor executor, Collection<Modifier> col) {
		synchronized (col) {
			for (Modifier c : col) {
				try {
					// If the host in the modifier is null, it is applicable for all containers
					// If c.getHost() != host, then this modifier does not belong to that container, so skip it
					if ((c.getHost() == null) || c.getHost().equals(container.getNode().getHost())) {
						// If executor is not null, then set the executor to the modifier so that it will know it should do it on remote
						if (executor != null) {
							c.setExecutor(executor);
						}
						log.debug("Executing modifier {}.", c);
						c.execute(container);

						// Unset the executor so that we will not have multiple instances of one modifier in the collection
						if (executor != null) {
							c.setExecutor(null);
						}
					}
				} catch (Exception e) {
					log.error("Failed to execute modifiers.", e);
					throw new FaframException(e);
				}
			}
		}
	}

	/**
	 * Clears the modifiers.
	 */
	public static void clearAllModifiers() {
		// Force the initialization
		getInstance();

		// Clear all the modifiers at the end so that they will not stay here when executing multiple tests
		modifiers.clear();
		postModifiers.clear();
		customModifiers.clear();
	}

	/**
	 * Gets RootContainer with given host (Fafram8 doesn't support 2 root containers on the same node).
	 *
	 * @param host host
	 * @return root container with given host
	 */
	public static Container getRootContainerByHost(String host) {
		for (Container container : ContainerManager.getContainerList()) {
			if (container instanceof RootContainer) {
				if (host.equals(container.getNode().getHost())) {
					return container;
				}
			}
		}
		throw new FaframException("Container with given host doesn't exist!");
	}
}
