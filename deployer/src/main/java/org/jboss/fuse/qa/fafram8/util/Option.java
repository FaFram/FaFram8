package org.jboss.fuse.qa.fafram8.util;

import lombok.Getter;

/**
 * Container option enum class.
 * Created by avano on 22.6.16.
 */
public enum Option {
	RESOLVER(true) {
		@Override
		public String toString() {
			return "--resolver";
		}
	},
	DATASTORE_OPTION(false) {
		@Override
		public String toString() {
			return "--datastore-option";
		}
	},
	MANUAL_IP(true) {
		@Override
		public String toString() {
			return "--manual-ip";
		}
	},
	PROFILE(false) {
		@Override
		public String toString() {
			return "--profile";
		}
	},
	BIND_ADDRESS(true) {
		@Override
		public String toString() {
			return "--bind-address";
		}
	},
	ZOOKEEPER_PASSWORD(true) {
		@Override
		public String toString() {
			return "--zookeeper-password";
		}
	},
	JVM_OPTS(false) {
		@Override
		public String toString() {
			return "--jvm-opts";
		}
	},
	JVM_MEM_OPTS(true) {
		@Override
		public String toString() {
			return "jvmMemOpts";
		}
	},
	VERSION(true) {
		@Override
		public String toString() {
			return "--version";
		}
	},
	PROXY_URI(true) {
		@Override
		public String toString() {
			return "--proxy-uri";
		}
	},
	PASS_PHRASE(true) {
		@Override
		public String toString() {
			return "--pass-phrase";
		}
	},
	DISABLE_DISTRIBUTION_UPLOAD(true) {
		@Override
		public String toString() {
			return "--disable-distribution-upload";
		}
	},
	PORT(true) {
		@Override
		public String toString() {
			return "--port";
		}
	},
	HOST(true) {
		@Override
		public String toString() {
			return "--host";
		}
	},
	PRIVATE_KEY(true) {
		@Override
		public String toString() {
			return "--private-key";
		}
	},
	MIN_PORT(true) {
		@Override
		public String toString() {
			return "--min-port";
		}
	},
	ENV(false) {
		@Override
		public String toString() {
			return "--env";
		}
	},
	SSH_RETRIES(true) {
		@Override
		public String toString() {
			return "--ssh-retries";
		}
	},
	PATH(true) {
		@Override
		public String toString() {
			return "--path";
		}
	},
	MAX_PORT(true) {
		@Override
		public String toString() {
			return "--max-port";
		}
	},
	FALLBACK_REPOS(false) {
		@Override
		public String toString() {
			return "--fallback-repos";
		}
	},
	WITH_ADMIN_ACCESS(true) {
		@Override
		public String toString() {
			return "--with-admin-access";
		}
	},
	COMMANDS(false) {
		@Override
		public String toString() {
			return "commands";
		}
	},
	BUNDLES(false) {
		@Override
		public String toString() {
			return "bundles";
		}
	},
	FABRIC_CREATE(true) {
		@Override
		public String toString() {
			return "fabric-create";
		}
	},
	// These are added manually, so they are not starting with -- (and therefore not included in getCommand)
	JMX_USER(true) {
		@Override
		public String toString() {
			return "jmx-user";
		}
	},
	JMX_PASSWORD(true) {
		@Override
		public String toString() {
			return "jmx-password";
		}
	},
	USER(true) {
		@Override
		public String toString() {
			return "user";
		}
	},
	PASSWORD(true) {
		@Override
		public String toString() {
			return "password";
		}
	},
	WORKING_DIRECTORY(true) {
		@Override
		public String toString() {
			return "working-dir";
		}
	},
	// For backwards compatibility to use with Builder.opts
	OTHER(false) {
		@Override
		public String toString() {
			return "";
		}
	};

	@Getter
	// If the option is single valued - if so, the next setting will overwrite it instead of append
	private boolean singleValued = false;

	Option(boolean singleValued) {
		this.singleValued = singleValued;
	}
}
