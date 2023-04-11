import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.PublicKey;
//import java.util.*;


public class MerkleTree {
    private Map<String, Account> accounts= new HashMap<>();
    private MerkleTree merkleTree;
    private MerkleNode root;    

    public MerkleTree(byte[][] hashes) {
        root = buildTree(hashes);
    }

    public byte[] getRootHash() {
        return root.getHash();
    }

    public static byte[] computeHash(byte[] left, byte[] right) throws Exception{
        byte[] data = new byte[left.length + right.length];
        System.arraycopy(left, 0, data, 0, left.length);
        System.arraycopy(right, 0, data, left.length, right.length);
        return hash(data);
    }
    private static byte[] hash(byte[] data) throws Exception{
        // your hash function implementation here
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.update(data);
        return sha.digest();
    }

    public MerkleProof getProof(byte[] leafHash) {
        MerkleProof proof = new MerkleProof();
        MerkleNode node = findNode(root, leafHash);
        if (node != null) {
            proof.setLeafHash(leafHash);
            while (node.getParent() != null) {
                MerkleNode parent = node.getParent();
                if (parent.getLeftChild() == node) {
                    proof.addSiblingHash(parent.getRightChild().getHash());
                } else {
                    proof.addSiblingHash(parent.getLeftChild().getHash());
                }
                node = parent;
            }
        }
        return proof;
    }

    private MerkleNode buildTree(byte[][] hashes) {
        MerkleNode[] nodes = new MerkleNode[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            nodes[i] = new MerkleNode(hashes[i]);
        }
        int numLevels = (int) Math.ceil(Math.log(nodes.length) / Math.log(2));
        for (int level = 0; level < numLevels; level++) {
            int numNodesAtLevel = (int) Math.pow(2, numLevels - level - 1);
            for (int i = 0; i < numNodesAtLevel; i++) {
                int leftChildIndex = 2 * i;
                int rightChildIndex = 2 * i + 1;
                MerkleNode leftChild = nodes[leftChildIndex];
                MerkleNode rightChild = (rightChildIndex < nodes.length) ? nodes[rightChildIndex] : null;
                MerkleNode parent = new MerkleNode(leftChild.getHash(), rightChild != null ? rightChild.getHash() : null);
                parent.setLeftChild(leftChild);
                if (rightChild != null) {
                    parent.setRightChild(rightChild);
                }
                leftChild.setParent(parent);
                if (rightChild != null) {
                    rightChild.setParent(parent);
                }
                nodes[nodes.length - numNodesAtLevel + i] = parent;
            }
        }
        return nodes[nodes.length - 1];
    }

    private MerkleNode findNode(MerkleNode node, byte[] hash) {
        if (node.isLeaf()) {
            return node.getHash().equals(hash) ? node : null;
        } else {
            MerkleNode left = findNode(node.getLeftChild(), hash);
            MerkleNode right = (left != null) ? null : findNode(node.getRightChild(), hash);
            return (left != null) ? left : right;
        }
    }
        
    

    public class Account {
    
        private PublicKey publicKey;
        private String clientName;
        private int value;
        private byte[] stateHash;
        

        public Account(PublicKey publicKey, String clientName, String value) {
            this.publicKey = publicKey;
            this.clientName = clientName;
            this.value = Integer.parseInt(value);
        }

        
    

        public byte[] getAccountState(String accountId) {
            if (!accounts.containsKey(accountId)) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }
            Account account = accounts.get(accountId);
            return account.getStateHash();
        }

        public MerkleProof getAccountStateProof(String accountId) {
            if (!accounts.containsKey(accountId)) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }
            Account account = accounts.get(accountId);
            return merkleTree.getProof(account.getStateHash());
        }


        public PublicKey getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

         
        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public byte[] getStateHash() {
            return stateHash;
        }

        
        
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        
    }

    private static class MerkleNode {
        private byte[] hash;
        private MerkleNode parent;
        private MerkleNode leftChild;
        private MerkleNode rightChild;

        public MerkleNode(byte[] hash) {
            this.hash = hash;
        }

        public MerkleNode(byte[] leftHash, byte[] rightHash) {
            try{
                this.hash = computeHash(leftHash, rightHash);
            }catch(Exception e){
                e.printStackTrace();
            }
            
        }

        public byte[] getHash() {
            return hash;
        }

        public MerkleNode getParent() {
            return parent;
        }

        public void setParent(MerkleNode parent) {
            this.parent = parent;
        }

        public MerkleNode getLeftChild() {
            return leftChild;
        }

        public void setLeftChild(MerkleNode leftChild) {
            this.leftChild = leftChild;
        }

        public MerkleNode getRightChild() {
            return rightChild;
        }

        public void setRightChild(MerkleNode rightChild) {
            this.rightChild = rightChild;
        }

        public boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }
    }

    public static class MerkleProof {
        private byte[] leafHash;
        private byte[][] siblingHashes;

        public byte[] getLeafHash() {
            return leafHash;
        }

        public void setLeafHash(byte[] leafHash) {
            this.leafHash = leafHash;
        }

        public byte[][] getSiblingHashes() {
            return siblingHashes;
        }

        public void addSiblingHash(byte[] siblingHash) {
            if (siblingHashes == null) {
                siblingHashes = new byte[1][];
            } else {
                byte[][] newSiblingHashes = new byte[siblingHashes.length + 1][];
                System.arraycopy(siblingHashes, 0, newSiblingHashes, 0, siblingHashes.length);
                siblingHashes = newSiblingHashes;
            }
            siblingHashes[siblingHashes.length - 1] = siblingHash;
        }
    }
}

