package org.jboss.fuse.qa.fafram8.ssh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.jboss.fuse.qa.fafram8.exceptions.CopyFileException;
import org.jboss.fuse.qa.fafram8.exceptions.KarafSessionDownException;
import org.jboss.fuse.qa.fafram8.exceptions.SSHClientException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Specific SSHClient for connecting to Node.
 *
 * @author : Roman Jakubco (rjakubco@redhat.com)
 */
@Slf4j
public class NodeSSHClient extends SSHClient {
	@Override
	public String executeCommand(String command, boolean suppressLog) throws KarafSessionDownException, SSHClientException {
		return executeCommand(command, suppressLog, false);
	}

	@Override
	public String executeCommand(String command, boolean suppressLog, boolean ignoreExceptions) throws KarafSessionDownException,
			SSHClientException {
		String returnString;

		if (!suppressLog) {
			log.debug("Command: " + command);
		}
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			try (InputStream in = channel.getInputStream()) {
				channel.connect();
				returnString = convertStreamToString(in);
			}
			returnString = returnString.replaceAll("\u001B\\[[;\\d]*m", "").trim();
			channel.disconnect();
			return returnString;
		} catch (JSchException ex) {
			if (!ignoreExceptions) {
				log.error("Cannot execute ssh command: \"" + command + "\"", ex);
			}
			throw new SSHClientException(ex);
		} catch (IOException ex) {
			if (!ignoreExceptions) {
				log.error(ex.getLocalizedMessage());
			}
			throw new SSHClientException(ex);
		}
	}

	/**
	 * Copies a file to the remote machine.
	 *
	 * @param localPath local path to file
	 * @param remotePath remote path to file
	 * @throws CopyFileException if there was problem with copying the file
	 */
	public void copyFileToRemote(String localPath, String remotePath) throws CopyFileException {
		log.info("Copying file " + localPath + " to remote machine path " + remotePath);

		try {
			final ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			final File file = new File(localPath);
			sftpChannel.cd(StringUtils.substringBeforeLast(remotePath, "/"));
			try (FileInputStream fis = new FileInputStream(file)) {
				sftpChannel.put(fis, StringUtils.substringAfterLast(remotePath, "/"));
			}
			sftpChannel.disconnect();
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Exception thrown during uploading file to remote machine");
			throw new CopyFileException(ex);
		}
	}

	/**
	 * Convert remote file to String.
	 *
	 * @param remotePath absolute path to remote file
	 * @return content of remote file as String
	 * @throws CopyFileException if copy fails
	 */
	public String readFileFromRemote(String remotePath) throws CopyFileException {
		log.info("Reading file from remote machine path " + remotePath);

		final ChannelSftp sftpChannel;
		String propertyFileString;

		try {
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			try (InputStream stream = sftpChannel.get(remotePath)) {
				propertyFileString = IOUtils.toString(stream);
			}
			sftpChannel.disconnect();
		} catch (RuntimeException ex) {
			log.error("Exception thrown during reading file from remote machine ", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Exception thrown during reading file from remote machine ", ex);
			throw new CopyFileException(ex);
		}
		return propertyFileString;
	}

	/**
	 * Writes input stream to file on remote machine.
	 *
	 * @param stream input stream that should be uploaded
	 * @param remotePath path to file on remote
	 * @throws CopyFileException if there was problem with uploading the file
	 */
	public void writeFileToRemote(InputStream stream, String remotePath) throws CopyFileException {
		log.info("Writing file to remote machine path " + remotePath);

		try {
			final ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			sftpChannel.cd(StringUtils.substringBeforeLast(remotePath, "/"));
			sftpChannel.put(stream, StringUtils.substringAfterLast(remotePath, "/"));

			sftpChannel.disconnect();
		} catch (RuntimeException ex) {
			log.error("Exception thrown during uploading file to remote machine ", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Exception thrown during uploading file to remote machine ", ex);
			throw new CopyFileException(ex);
		}
	}
}
