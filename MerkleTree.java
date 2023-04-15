import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.*;
import java.nio.ByteBuffer;


public class MerkleTree {
    public static Map<String, Account> accounts;
    
    private static List<MerkleNode> roots;
      

    public MerkleTree(Map<String, Account> Accounts) throws Exception{
        accounts=Accounts;
        byte[][] hashes=computeAccountHashes();
        
        roots = buildTree(hashes);
        
        
        
    }

    private byte[][] computeAccountHashes() throws Exception {
        List<byte[]> accountHashes = new ArrayList<>();
        for (Account account : accounts.values()) {
            byte[] data = new byte[account.getPublicKey().getEncoded().length + account.getClientName().getBytes().length + 4];
            System.arraycopy(account.getPublicKey().getEncoded(), 0, data, 0, account.getPublicKey().getEncoded().length);
            System.arraycopy(account.getClientName().getBytes(), 0, data, account.getPublicKey().getEncoded().length, account.getClientName().getBytes().length);
            ByteBuffer.wrap(data, data.length-4, 4).putInt(account.getValue());
            byte[] accountHash = hash(data);
            
            
            account.setAccountHash(accountHash);
            accountHashes.add(accountHash);
            
           
        }
        return accountHashes.toArray(new byte[0][]);
    }

    public static byte[] computeHash(byte[] left, byte[] right) throws Exception {
        byte[] data = new byte[left.length + right.length];
        System.arraycopy(left, 0, data, 0, left.length);
        System.arraycopy(right, 0, data, left.length, right.length);
        return hash(data);
    }

    public static byte[] hash(byte[] data) throws Exception{
        // your hash function implementation here
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.update(data);

        
        return sha.digest();
    }

    public MerkleProof getProof(byte[] leafHash) {
        MerkleProof proof = new MerkleProof();
        MerkleNode node=null;
        for(MerkleNode root: roots){
            node = findNode(root, leafHash);
            if(node!=null){
                proof.rootHash=root.getHash();
                break;
            }
                
        }
        
        
        if (node != null) {
            //System.out.println("basas");
            proof.setLeafHash(leafHash);
            while (node.getParent() != null) {
                MerkleNode parent = node.getParent();
                if (parent.getLeftChild() == node) {
                    proof.addSiblingHash(parent.getRightChild().getHash(),false);
                } else {
                    proof.addSiblingHash(parent.getLeftChild().getHash(),true);
                }
                node = parent;
            }
        }
        return proof;
    }
    
    private List<MerkleNode> buildTree(byte[][] hashes) throws Exception{
        List<MerkleNode> roots=new ArrayList<>();
        int j=0;
        int k;
        int ind=0;
        
        MerkleNode[] nodes = new MerkleNode[hashes.length*2];
        //MerkleNode[] parents= new MerkleNode[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            nodes[i] = new MerkleNode(hashes[i]);
        }
        k=hashes.length;
        if(k==1){
            roots.add(nodes[0]);
            return roots;
        } 
        if(k%2!=0){
            roots.add(nodes[k-1]);
            k--;
        }
        

        while(k%2==0){
            int aux=0;
            
            for(int i=ind;i<k;i+=2){
                MerkleNode leftChild=nodes[i];
                MerkleNode rightChild=nodes[i+1];
                MerkleNode parent = new MerkleNode(leftChild.getHash(),rightChild.getHash());
                parent.setLeftChild(leftChild);
                parent.setRightChild(rightChild);

                leftChild.setParent(parent);
                rightChild.setParent(parent);
                nodes[hashes.length+j]=parent;
                j++;
                aux++;
            }
            ind=k;
            k=ind+aux;

        }
        System.out.println("tamanho "+(hashes.length+j-1));
        roots.add(nodes[hashes.length+j-1]);

        return roots;
        //return nodes[hashes.length+j-1];


        
       
        
    }

    private MerkleNode findNode(MerkleNode node, byte[] hash) {
        
        if (node.isLeaf()) {
            //System.out.println(node.getHash().equals(hash));
            return node.getHash().equals(hash) ? node : null;
        } else {
            //System.out.println("boas2");
            MerkleNode left = findNode(node.getLeftChild(), hash);
            MerkleNode right = (left != null) ? null : findNode(node.getRightChild(), hash);
            return (left != null) ? left : right;
        }
        
    }
    
    

    public static class MerkleNode {
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
                //System.out.println(Base64.getEncoder().encodeToString(this.hash) );
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
        private boolean[] isLeft= new boolean[20];
        private byte[] rootHash;

        public MerkleProof(byte[] leafHash, byte[][] siblingHashes, boolean[] isLeft, byte[] rootHash){
            this.leafHash=leafHash;
            this.siblingHashes=siblingHashes;
            this.isLeft=isLeft;
            this.rootHash=rootHash;
        }

        public MerkleProof(){

        }

        public void setRootHash(byte[] rootHash){
            this.rootHash=rootHash;
        }
        public byte[] getRootHash(){
            return rootHash;
        }
        public byte[] getLeafHash() {
            return leafHash;
        }

        public void setLeafHash(byte[] leafHash) {
            this.leafHash = leafHash;
        }

        public boolean[] getLefts(){
            return isLeft;
        }

        public byte[][] getSiblingHashes() {
            return siblingHashes;
        }

        public void addSiblingHash(byte[] siblingHash,boolean left) {
            if (siblingHashes == null) {
                siblingHashes = new byte[1][];
                
            } else {
                byte[][] newSiblingHashes = new byte[siblingHashes.length + 1][];
                System.arraycopy(siblingHashes, 0, newSiblingHashes, 0, siblingHashes.length);
                siblingHashes = newSiblingHashes;
            }
            siblingHashes[siblingHashes.length - 1] = siblingHash;
            isLeft[siblingHashes.length-1]=left;
        }
    }

    public static boolean verifyProof(  MerkleProof proof) throws Exception {
        
        int i=0;
        byte[] computedHash = proof.leafHash;
        if(proof.siblingHashes!=null){
            for (byte[] siblingHash : proof.siblingHashes) {
                
                if ((computedHash == null) || (siblingHash == null)) {
                    //System.out.println("bazoooo");
                    return false;
                }
                System.out.println("false or not: "+proof.isLeft[i]);

                if(proof.isLeft[i]){
                    computedHash=computeHash(siblingHash,computedHash);
                }else{
                    computedHash=computeHash(computedHash,siblingHash);
                }
                
                
                
                
                i++;
            }
        }
        
        //System.out.println(Base64.getEncoder().encodeToString(computedHash) );
        //System.out.println(Base64.getEncoder().encodeToString(root.getHash()) );
        return MessageDigest.isEqual(proof.rootHash, computedHash);
    }

   
    public static String getName(PublicKey pubKey){
        for(Account account: accounts.values()){
            if(account.getPublicKey().equals(pubKey)){
                return account.getClientName();
            }
        }
        return null;
    }

    

    
}

