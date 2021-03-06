/*
 * Copyright 2012 - 2014 Eric Myhre <http://exultant.us>
 *
 * This file is part of mdm <https://github.com/heavenlyhash/mdm/>.
 *
 * mdm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.polydawn.mdm.commands;

import java.io.*;
import java.util.*;
import net.polydawn.mdm.*;
import net.polydawn.mdm.errors.*;
import net.sourceforge.argparse4j.inf.*;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import static net.polydawn.mdm.Loco.*;
import static us.exultant.ahs.util.Strings.join;

public class MdmUpdateCommand extends MdmCommand {
	public MdmUpdateCommand(Repository repo) {
		super(repo);
	}

	public void parse(Namespace args) {}

	public void validate() throws MdmExitMessage {}

	public MdmExitMessage call() throws ConfigInvalidException, IOException {
		try {
			assertInRepo();
		} catch (MdmExitMessage e) { return e; }

		MdmModuleSet moduleSet;
		try {
			moduleSet = new MdmModuleSet(repo);
		} catch (ConfigInvalidException e) {
			throw new MdmExitInvalidConfig(Constants.DOT_GIT_MODULES);
		}
		Map<String,MdmModuleDependency> modules = moduleSet.getDependencyModules();

		// Go over every module and do what we can to it, keeping a list of who each kind of operation was performed on for summary output later.
		List<MdmModule> impacted = new ArrayList<MdmModule>();
		List<MdmModule> unphased = new ArrayList<MdmModule>();
		List<MdmModule> contorted = new ArrayList<MdmModule>();
		int hashMismatchWarnings = 0;
		int i = 0;
		boolean fancy = System.console() != null;
		for (MdmModuleDependency module : modules.values()) {
			i++;
			try {
				os.print((fancy ? "\033[2K\r" : "") + "updating module "+i+" of "+modules.size()+": "+module.getHandle() +" ..." + (fancy ? "" : "\n"));
				if (Plumbing.fetch(repo, module)) {
					impacted.add(module);
					if (!module.getRepo().resolve(Constants.HEAD).equals(module.getIndexId())) {
						// in putting the module to the version named in .gitmodules, we made it disagree with the parent index.
						// this probably indicates oddness.
						hashMismatchWarnings++;
						os.println((fancy ? "\033[2K\r" : "") + "notice: in updating "+module.getHandle()+" to version "+module.getVersionName()+", mdm left the submodule with a different hash checked out than the parent repo expected.");
					}
				} else
					unphased.add(module);
			} catch (MdmException e) {
				os.println((fancy ? "\033[2K\r" : "") + "error: in updating "+module.getHandle()+" to version "+module.getVersionName()+", "+e);
				contorted.add(module);
			}
		}
		os.print((fancy ? "\033[2K\r" : ""));

		// explain notices about hash mismatches, if any occured.
		if (hashMismatchWarnings > 0) {
			os.println();
			os.println("warnings about submodule checkouts not matching the hash expected by the parent repo may indicate a problem which you should investigate immediately to make sure your dependencies are repeatable to others.");
			os.println("this issue may be because the repository you are fetching from has moved what commit the version branch points to (which is cause for concern), or it may be a local misconfiguration (did you resolve a merge conflict recently?  audit your log; the version name in gitmodules config must move at the same time as the submodule hash).");
			os.println();
		} else if (contorted.size() > 0) {
			os.println();
		}


		// That's all.  Compose a status string.
		StringBuilder status = new StringBuilder();
		status.append("mdm dependencies have been updated (");
		status.append(impacted.size()).append(" changed, ");
		status.append(unphased.size()).append(" unaffected");
		if (contorted.size() > 0)
			status.append(", ").append(contorted.size()).append(" contorted");
		status.append(")");
		if (impacted.size() > 0)
			status.append("\n  changed: \t").append(join(toHandles(impacted), "\n\t\t"));
		if (contorted.size() > 0)
			status.append("\n  contorted: \t").append(join(toHandles(contorted), "\n\t\t"));

		return new MdmExitMessage(contorted.size() > 0 ? ":(" : ":D", status.toString());
	}
}
