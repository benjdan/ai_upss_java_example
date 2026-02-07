package com.upss.middleware;

public interface SecurityMiddleware {
    
    void process(PipelineResult result);

    String getName();
}
