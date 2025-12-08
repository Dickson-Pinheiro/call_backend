package com.group_call.call_backend.controller;

import com.group_call.call_backend.dto.TreeNodeInfo;
import com.group_call.call_backend.tree.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/trees")
@Transactional(readOnly = true)
public class TreeVisualizationController {

    private final UserTree userTree;
    private final CallTree callTree;
    private final ChatMessageTree chatMessageTree;
    private final CallRatingTree callRatingTree;
    private final FollowTree followTree;

    public TreeVisualizationController(UserTree userTree, CallTree callTree,
                                       ChatMessageTree chatMessageTree, CallRatingTree callRatingTree,
                                       FollowTree followTree) {
        this.userTree = userTree;
        this.callTree = callTree;
        this.chatMessageTree = chatMessageTree;
        this.callRatingTree = callRatingTree;
        this.followTree = followTree;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTrees() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("userTree", buildTreeStructure("UserTree", userTree));
        response.put("callTree", buildTreeStructure("CallTree", callTree));
        response.put("chatMessageTree", buildTreeStructure("ChatMessageTree", chatMessageTree));
        response.put("callRatingTree", buildTreeStructure("CallRatingTree", callRatingTree));
        response.put("followTree", buildTreeStructure("FollowTree", followTree));
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserTree() {
        return ResponseEntity.ok(buildTreeStructure("UserTree", userTree));
    }

    @GetMapping("/calls")
    public ResponseEntity<Map<String, Object>> getCallTree() {
        return ResponseEntity.ok(buildTreeStructure("CallTree", callTree));
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getChatMessageTree() {
        return ResponseEntity.ok(buildTreeStructure("ChatMessageTree", chatMessageTree));
    }

    @GetMapping("/ratings")
    public ResponseEntity<Map<String, Object>> getCallRatingTree() {
        return ResponseEntity.ok(buildTreeStructure("CallRatingTree", callRatingTree));
    }

    @GetMapping("/follows")
    public ResponseEntity<Map<String, Object>> getFollowTree() {
        return ResponseEntity.ok(buildTreeStructure("FollowTree", followTree));
    }

    private <T> Map<String, Object> buildTreeStructure(String treeName, AVLTree<T> tree) {
        Map<String, Object> structure = new HashMap<>();
        
        structure.put("treeName", treeName);
        structure.put("totalNodes", tree.size());
        structure.put("treeHeight", tree.getRoot() != null ? tree.getRoot().getHeight() : 0);
        structure.put("isEmpty", tree.isEmpty());
        
        List<TreeNodeInfo> nodes = new ArrayList<>();
        if (tree.getRoot() != null) {
            collectNodeInfo(tree.getRoot(), null, nodes);
        }
        structure.put("nodes", nodes);
        
        structure.put("inOrderTraversal", tree.getInOrderKeys());
        structure.put("preOrderTraversal", tree.getPreOrderKeys());
        structure.put("postOrderTraversal", tree.getPostOrderKeys());
        
        return structure;
    }

    private <T> void collectNodeInfo(AVLNode<T> node, Long parentKey, List<TreeNodeInfo> nodes) {
        if (node == null) {
            return;
        }

        TreeNodeInfo info = new TreeNodeInfo();
        info.setKey(node.getKey());
        info.setData(simplifyData(node.getData()));
        info.setParentKey(parentKey);
        info.setLeftChildKey(node.getLeft() != null ? node.getLeft().getKey() : null);
        info.setRightChildKey(node.getRight() != null ? node.getRight().getKey() : null);
        info.setHeight(node.getHeight());
        
        int leftHeight = node.getLeft() != null ? node.getLeft().getHeight() : 0;
        int rightHeight = node.getRight() != null ? node.getRight().getHeight() : 0;
        info.setBalanceFactor(leftHeight - rightHeight);
        
        nodes.add(info);

        collectNodeInfo(node.getLeft(), node.getKey(), nodes);
        collectNodeInfo(node.getRight(), node.getKey(), nodes);
    }

    private Object simplifyData(Object data) {
        if (data == null) {
            return null;
        }
        
        Map<String, Object> simplified = new HashMap<>();
        String className = data.getClass().getSimpleName();
        simplified.put("type", className);
        simplified.put("info", "Entity: " + className);
        
        return simplified;
    }
}
