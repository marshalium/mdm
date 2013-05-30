/*
 * Copyright 2012, 2013 Eric Myhre <http://exultant.us>
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

package us.exultant.mdm;

import java.io.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.*;
import us.exultant.mdm.errors.*;

public class MdmReleaseRepo /* extends MdmModule */ {
	/**
	 * Open the repo at given path, throw if there's no repo or if it's missing the
	 * telltales of being an mdm release repo.
	 *
	 * @param relRepoPath path to repository to load.  Doubles as name.
	 * @return a repository that smells like a proper mdm release repo.
	 * @throws MdmRepositoryNonexistant if there's no repository there
	 * @throws MdmRepositoryIOException if there were errors reading the repository
	 * @throws MdmModuleTypeException if the repository doesn't look like an {@link MdmReleaseRepo}.
	 */
	public static Repository load(String relRepoPath) throws MdmRepositoryNonexistant, MdmRepositoryIOException, MdmModuleTypeException {
		try {
			Repository relRepo = new FileRepositoryBuilder()
				.setWorkTree(new File(relRepoPath).getCanonicalFile())	// must use getCanonicalFile to work around https://bugs.eclipse.org/bugs/show_bug.cgi?id=407478
				.build();
			if (relRepo == null)						// check that releases-repo is a git repo at all
				throw new MdmRepositoryNonexistant(relRepoPath);
			if (relRepo.getRef("refs/heads/mdm/init") == null)		// check that the releases-repo has the branches we expect from an mdm releases repo
				throw new MdmModuleReleaseNeedsBranch(relRepoPath, "mdm/init");
			if (relRepo.getRef("refs/heads/master") == null)		// check that the releases-repo has the branches we expect from an mdm releases repo
				throw new MdmModuleReleaseNeedsBranch(relRepoPath, "master");
			return relRepo;
		} catch (IOException e) {
			throw new MdmRepositoryIOException(false, relRepoPath, e);
		}
	}

	public static class MdmModuleReleaseNeedsBranch extends MdmModuleTypeException {
		public MdmModuleReleaseNeedsBranch(String relRepoPath, String branchName) {
			super("releases-repo directory '"+relRepoPath+"' contains a git repo, but it doesn't look like something that's been set up for mdm releases.\n(There's no branch named \""+branchName+"\", and there should be.)");
		}
	}
}
