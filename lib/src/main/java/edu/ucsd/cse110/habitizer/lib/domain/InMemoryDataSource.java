package edu.ucsd.cse110.habitizer.lib.domain;

import java.util.HashMap;
import java.util.Map;

import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.Task;

public class InMemoryDataSource {
    // tasks have their own observers
    private final Map<Integer, Task> tasks = new HashMap<>();

    public InMemoryDataSource() { }
}