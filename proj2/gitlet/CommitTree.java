package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents gitlet version history as a tree.
 * Adding files copies the files specified in the java gitlet.Main add []
 * command to the staging area directory. Each staged file is named using
 * SHA-1 while the workspace file name (e.g. file.txt) is stored in a map.
 * Committing files moves all files in the staging area directory to the
 * commits directory. The references that map the filename (e.g. file.txt) to the
 * SHA-1 id (e.g. f9324c...) are passed to a new commit node. That commit node is
 * then added to the end of the current head node and the head pointer is moved
 * forward.
 */
public class CommitTree implements Serializable {

    static final File COMMIT_LOG = Utils.join(Main.METADATA_DIR, "commit.dat");
    static final File STAGING_AREA = Utils.join(Main.METADATA_DIR, "stage");
    static final File COMMITS = Utils.join(Main.METADATA_DIR, "commits");
    static final int SEARCH_PRECISION = 7;

    /**
     * Represents a single commit in the version history.
     */
    public class CommitNode implements Serializable {
        private String message;
        private Date timestamp;
        private String id;
        private HashMap<String, String> references;
        public CommitNode parent;
        public List<CommitNode> children;

        /**
         * @param message the commit message that displays when using gitet log
         * @param timestamp the current date and time
         */
        public CommitNode(String message, Date timestamp,
                          HashMap<String, String> references) {

            this.message = message;
            this.timestamp = timestamp;
            this.children = new LinkedList<>();
            if (references == null) {
               this.references = new HashMap<>();
            }
            else {
                this.references = references;
            }

            String uniqueId = Arrays.stream(this.references.values().toArray())
                    .map(e -> e.toString()).reduce("", String::concat);
            id = Utils.sha1(uniqueId + timestamp.toString());
        }

        /**
         * Returns the string representation of a commit as per the specifications of
         * the gitlet log command. Log should look like:
         * ===
         * commit e05e0c60f7945913cdfa6692855c3ba7aec706c6
         * Date: Wed Dec 31 16:00:00 1969 -0800
         * initial commit
         * @return the string representation of the commit
         */
        public String toString() {
            SimpleDateFormat format
                    = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            String date = format.format(timestamp);
            String s = "";
            s += "===\n"
              + "commit " + id + "\n"
              + "Date: " + date + "\n"
              + message + "\n\n";
            return s;
        }

        public void printAll() {
            for (CommitNode n: children) {
                n.printAll();
            }
            System.out.println(this);
        }

        /**
         * Returns whether or not the current commit has saved a version of a file.
         * @param name the filename as it appears in the local workspace.
         * @param id the id of the file as determine by a SHA-1 conversion of the
         *           file name + file contents.
         * @return
         */
        public boolean contains(String name, String id) {
            if (references.containsKey(name)) {
                return references.get(name).equals(id);
            }
            return false;
        }

        public void addChild(CommitNode node) {
            node.parent = this;
            children.add(node);
        }

        public void findCommitsWithMessage(String message, LinkedList<String> ids) {
            if (this.message.equals(message)) {
                ids.push(this.id);
            }
            for (CommitNode n: children) {
                n.findCommitsWithMessage(message, ids);
            }
        }

        /**
         * @source https://stackoverflow.com/questions/2261697/compare-first-three-characters-of-two-strings
         * @param id
         * @param precision
         * @return
         */
        public CommitNode findCommit(String id, int precision) {
            if (id.length() < precision) {
                Main.exitWithError("You must supply an id of length " + SEARCH_PRECISION + "or longer.");
            }
            if (this.id.startsWith(id.substring(0, precision - 1))) {
                return this;
            }
            for (var n: children) {
                CommitNode candidate = n.findCommit(id, precision);
                if (candidate != null) {
                    return candidate;
                }
            }
            return null;
        }

    }

    private String currentBranch;
    private HashMap<String, CommitNode> branches = new HashMap<>();
    // Holds the references to blobs containing 1) the file name 2) the unique id
    private HashMap<String, String> staged = new HashMap<>();
    private LinkedList<String> toRemove = new LinkedList<String>();
    private CommitNode root;

    public CommitTree() {
        root = new CommitNode("initial commit", new Date(0), null);
        branches.put("master", root);
        currentBranch = "master";
    }

    private CommitNode head() {
        return branches.get(currentBranch);
    }

    private void resetToCommit(CommitNode node) {
        File workingFile;
        File commitFile;

        // Copy and overwrite files from new branch into working directory
        for (String fileName: node.references.keySet()) {
            String fileID = node.references.get(fileName);
            commitFile = Utils.join(COMMITS, fileID);
            workingFile = Utils.join(Main.CWD, fileName);
            String contents = Utils.readContentsAsString(commitFile);
            Utils.writeContents(workingFile, contents);
        }

        // Delete file that are not tracked by the new branch
        deleteUntrackedFiles(node);
        //for (String fileName: Main.CWD.list()) {
        //    workingFile = Utils.join(Main.CWD, fileName);
        //    if (!workingFile.isDirectory()
        //            && !branch.references.containsKey(fileName)) {
        //        workingFile.delete();
        //    }
        //}
        clearStagingArea();
        branches.replace(currentBranch, node);
    }

    public void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            throw new RuntimeException(
                    "A branch with that name does not exist.");
        }
        else if (branchName.equals(currentBranch)) {
            throw new RuntimeException(
                    "Cannot remove the current branch.");
        }
        else {
            branches.remove(branchName);
        }
        save();
    }

    public void deleteUntrackedFiles(CommitNode node) {
        File workingFile;
        for (String fileName: Main.CWD.list()) {
            workingFile = Utils.join(Main.CWD, fileName);
            if (!workingFile.isDirectory()
                    && !node.references.containsKey(fileName)) {
                workingFile.delete();
            }
        }
    }

    public void reset(String id) {
       resetToCommit(findCommit(id));
       save();
    }

    private CommitNode findCommit(String id) {
        return root.findCommit(id, SEARCH_PRECISION);
    }

    private void checkoutFile(String fileName, CommitNode node) {
        if (!node.references.containsKey(fileName)) {
            throw new RuntimeException("File does not exist in that commit.");
        }
        File workingFile = Utils.join(Main.CWD, fileName);
        File commitFile = Utils.join(COMMITS, node.references.get(fileName));
        String contents = Utils.readContentsAsString(commitFile);
        Utils.writeContents(workingFile, contents);
    }

    // Methods for checkout command
    // ============================
    public void checkoutByFileName(String fileName) {
        checkoutFile(fileName, head());
    }

    public void checkoutByCommitID(String id, String fileName) {
        CommitNode node = findCommit(id);
        if (node == null) {
            Main.exitWithError("No commit with that id exists.");
        }
        checkoutFile(fileName, node);

    }

    public void checkoutBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            throw new RuntimeException("No such branch exists.");
        }
        else if (currentBranch == branchName) {
            throw new RuntimeException("No need to checkout the current branch.");
        }
        else if (getUntracked().size() > 0) {
            throw new RuntimeException("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
        }
        else {
            currentBranch = branchName;
            resetToCommit(branches.get(branchName));
        }
        save();
    }

    // Methods for comparing working directory to head commit
    // ======================================================
    public LinkedList<String> getDeleted() {
        LinkedList<String> deletedFiles = new LinkedList<>();
        File file;
        for (String fileName: head().references.keySet()) {
            file = Utils.join(Main.CWD, fileName);
            if (!file.exists() && !toRemove.contains(fileName)) {
                deletedFiles.push(fileName);
            }
        }
        return deletedFiles;
    }

    public LinkedList<String> getChanged() {
        LinkedList<String> modifiedFiles = new LinkedList<>();
        File file;
        for (String fileName: Main.CWD.list()) {
            file = Utils.join(Main.CWD, fileName);
            if (file.isDirectory()) {
                continue;
            }
            String contents = Utils.readContentsAsString(file);
            if (head().references.containsKey(fileName)
                    && !head().references.get(fileName).equals(Utils.sha1(fileName + contents))) {
                modifiedFiles.push(fileName);
            }
        }
        return modifiedFiles;
    }

    public LinkedList<String> getUntracked() {
        LinkedList<String> untrackedFiles = new LinkedList<>();
        File file;
        for (String fileName: Main.CWD.list()) {
            file = Utils.join(Main.CWD, fileName);
            if (file.isDirectory()) {
                continue;
            }
            if (!head().references.containsKey(fileName) &&
                !staged.containsKey(fileName)) {
                untrackedFiles.push(fileName);
            }
        }
        return untrackedFiles;
    }

    public LinkedList<String> getModified() {
        LinkedList modified = new LinkedList<>();
        for (String fileName: getDeleted()) {
            modified.add(fileName + " (deleted)");
        }
        for (String fileName: getChanged()) {
            modified.add(fileName + " (modified)");
        }
        return modified;
    }

    public void printBranches() {
        for (String b: branches.keySet().stream().sorted().collect(Collectors.toList())) {
            if (b.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(b);
        }
    }

    public void printCommitsWithMessage(String message)  {
        LinkedList<String> commitIds = new LinkedList<>();
        root.findCommitsWithMessage(message, commitIds);
        if (commitIds.size() == 0) {
            Main.exitWithError("Found no commit with that message.");
        }
        for (var id: commitIds) {
            System.out.println(id);
        }
    }

    // ======================================================


    public void status() {
        System.out.println("=== Branches ===");
        printBranches();
        System.out.println("\n=== Staged Files ===");
        printStrings(staged.keySet().stream().sorted().collect(Collectors.toList()));
        System.out.println("\n=== Removed Files ===");
        printStrings(toRemove.stream().sorted().collect(Collectors.toList()));
        System.out.println("\n=== Modifications Not Staged for Commit ===");
        printStrings(getModified().stream().sorted().collect(Collectors.toList()));
        System.out.println("\n=== Untracked Files ===");
        printStrings(getUntracked().stream().sorted().collect(Collectors.toList()));
    }

    private void printStrings(List<String> list) {
        for (String s: list) {
            System.out.println(s);
        }
    }

    private void clearStagingArea() {
        File f;
        for (String fileName: staged.values()) {
            f = Utils.join(STAGING_AREA, fileName);
            f.delete();
        }
        staged.clear();
    }

    public void branch(String branchName) throws Exception {
        if (branches.containsKey(branchName)) {
            throw new Exception("A branch with that name already exists");
        }
        branches.put(branchName, head());
        save();
    }

    /**
     * Load the current commit tree in .gitlet/commit.dat from bytes.
     * @return The current commit tree object tracking the working initialized
     * directory.
     */
    public static CommitTree load() {
        return Utils.readObject(COMMIT_LOG, CommitTree.class) ;
    }

    /**
     * Serialize and save the CommitTree object to a file.
     */
    public void save() {
        Utils.writeContents(COMMIT_LOG, Utils.serialize(this));
    }

    /**
     * Prints out the string representation of each commit from the current
     * head to the initial commit.
     */
    public void printLog() {
        CommitNode curr = head();
        while (curr != null) {
            System.out.print(curr.toString());
            // Red line in IntelliJ but still compiles?
            curr = curr.parent;
        }
    }

    public CommitNode findSplitPoint(CommitNode A, CommitNode B) {

        int dateDiff = A.timestamp.compareTo(B.timestamp);
       while (A != null && B != null) {
           if (A == B) {
               return A;
           }

           if (dateDiff < 0) {
               B = B.parent;
           }
           else {
               A = A.parent;
           }

       }
       return null;

    }

    public void printGlobalLog() {
        root.printAll();
    }


    /**
     * Creates a new commit node by overwriting parent commit with new changes.
     * @param message The commit message that should be displayed with the
     *                gitlet log command.
     */
    public void commit(String message) {

        // Loop over all staged docs
        for (String filename: staged.keySet()) {
            // Get unique SHA-1 associated with filename.
            String fileID = staged.get(filename);
            // Copy staged file to committed file.
            File oldFile = Utils.join(STAGING_AREA, fileID);
            File newFile = Utils.join(COMMITS, fileID);
            String contents = Utils.readContentsAsString(oldFile);
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(newFile, contents);
            //!// Delete staged file after copying
            oldFile.delete();
        }

        HashMap<String, String> newReferences = new HashMap<String, String>();
        newReferences.putAll(head().references);
        // Apply staged removals
        for (String r: toRemove) {
            newReferences.remove(r);
        }
        toRemove.clear();
        // Apply staged additions/modifications;
        newReferences.putAll(staged);
        staged.clear();

        // Create commit node with new data and update head().
        CommitNode newCommit = new CommitNode(
                                    message,
                                    new Date(System.currentTimeMillis()),
                                    newReferences);
        // Red line in IntelliJ but still compiles?
        head().addChild(newCommit);
        //head = newCommit;
        branches.replace(currentBranch, newCommit);
        save();
    }

    public void remove(String fileName) {
        File fileToRemove = Utils.join(Main.CWD, fileName);
        fileToRemove.delete();
        toRemove.push(fileName);
        if (staged.containsKey(fileName)) {
            File stagedFile = Utils.join(STAGING_AREA, staged.get(fileName));
            stagedFile.delete();
            staged.remove(fileName);
        }
        save();
    }

    /**
     * Adds files to .gitlet/staged and gives them unique ids.
     * @param files the files in the current workspace to be added to the staging
     *              area.
     */
    public void stage(File[] files) {

        File fileAsCopy;
        for (File f: files) {
            String contents = Utils.readContentsAsString(f);
            String id = Utils.sha1(f.getName() + contents);

            if (head().contains(f.getName(), id)) {
                continue;
            }

            fileAsCopy = Utils.join(STAGING_AREA, id);
            if (fileAsCopy.exists()) {
                fileAsCopy.delete();
            }
            try {
                fileAsCopy.createNewFile();
                Utils.writeContents(fileAsCopy,contents);
                // We probably don't need the if statement
                // since put just replaces the key anyways
                if (staged.containsKey(f.getName())) {
                    staged.replace(f.getName(), id);
                }
                else {
                    staged.put(f.getName(), id);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        save();
    }
}
