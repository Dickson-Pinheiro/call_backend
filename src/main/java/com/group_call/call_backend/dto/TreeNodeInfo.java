package com.group_call.call_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeInfo {
    private Long key;
    private Object data;
    private Long parentKey;
    private Long leftChildKey;
    private Long rightChildKey;
    private int height;
    private int balanceFactor;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class TreeStructureResponse {
    private String treeName;
    private int totalNodes;
    private int treeHeight;
    private List<TreeNodeInfo> nodes;
    private List<Long> inOrderTraversal;
    private List<Long> preOrderTraversal;
    private List<Long> postOrderTraversal;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class AllTreesResponse {
    private TreeStructureResponse userTree;
    private TreeStructureResponse callTree;
    private TreeStructureResponse chatMessageTree;
    private TreeStructureResponse callRatingTree;
    private TreeStructureResponse followTree;
    private String timestamp;
}
