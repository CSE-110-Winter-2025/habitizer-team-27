package edu.ucsd.cse110.habitizer.app;

import android.app.Application;
import android.util.Log;


import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

import edu.ucsd.cse110.habitizer.app.data.db.AppDatabase;
import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
import edu.ucsd.cse110.habitizer.app.data.db.RoutineEntity;
import edu.ucsd.cse110.habitizer.app.data.db.RoutineTaskCrossRef;
import edu.ucsd.cse110.habitizer.app.data.db.TaskEntity;
import edu.ucsd.cse110.habitizer.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.habitizer.lib.domain.Routine;
import edu.ucsd.cse110.habitizer.lib.domain.RoutineRepository;
import edu.ucsd.cse110.habitizer.lib.domain.Task;
import edu.ucsd.cse110.habitizer.lib.domain.TaskRepository;
import edu.ucsd.cse110.observables.MutableSubject;
import edu.ucsd.cse110.observables.Observer;
import edu.ucsd.cse110.observables.PlainMutableSubject;
import edu.ucsd.cse110.observables.Subject;

public class HabitizerApplication extends Application {
    private static final String TAG = "HabitizerApplication";
    private static HabitizerRepository repository;
    
    // Repositories for MainViewModel compatibility
    private RoutineRepository routineRepository;
    private TaskRepository taskRepository;
    
    // Observer tracking
    private static final AtomicInteger observerCount = new AtomicInteger(0);
    
    // Default data for initialization
    private static final List<Task> DEFAULT_MORNING_TASKS = List.of(
            new Task(0, "Shower", false),
            new Task(1, "Brush teeth", false),
            new Task(2, "Dress", false),
            new Task(3, "Make coffee", false),
            new Task(4, "Make lunch", false),
            new Task(5, "Dinner prep", false),
            new Task(6, "Pack bag", false)
    );

    private static final List<Task> DEFAULT_EVENING_TASKS = List.of(
            new Task(100, "Charge devices", false),
            new Task(101, "Prepare dinner", false),
            new Task(102, "Eat dinner", false),
            new Task(103, "Wash dishes", false),
            new Task(104, "Pack bag", false),
            new Task(105, "Homework", false)
    );

    public static HabitizerRepository getRepository() {
        return repository;
    }
    
    // Implementation of TaskRepository that uses HabitizerRepository
    private class HabitizerTaskRepository extends TaskRepository {
        private final int repositoryId;
        
        public HabitizerTaskRepository() {
            super(new InMemoryDataSource()); // This won't be used
            repositoryId = observerCount.incrementAndGet();
            Log.d(TAG, "Created HabitizerTaskRepository #" + repositoryId);
        }
        
        @Override
        public Subject<Task> find(int id) {
            Log.d(TAG, "TaskRepository #" + repositoryId + ": Looking for task with ID " + id);
            // Create a subject that observes the repository's task
            PlainMutableSubject<Task> subject = new PlainMutableSubject<>();
            
            // Initialize with current value if available
            List<Task> tasks = repository.getTasks().getValue();
            if (tasks != null) {
                for (Task task : tasks) {
                    if (task.getTaskId() == id) {
                        Log.d(TAG, "TaskRepository #" + repositoryId + ": Found task " + task.getTaskName());
                        subject.setValue(task);
                        break;
                    }
                }
            }
            
            // Observe repository changes
            int observerId = observerCount.incrementAndGet();
            Log.d(TAG, "TaskRepository #" + repositoryId + ": Creating task observer #" + observerId + " for task ID " + id);
            
            Observer<List<Task>> taskObserver = taskList -> {
                Log.d(TAG, "Task observer #" + observerId + " triggered with " + (taskList != null ? taskList.size() : 0) + " tasks");
                if (taskList != null) {
                    for (Task task : taskList) {
                        if (task.getTaskId() == id) {
                            Log.d(TAG, "Task observer #" + observerId + ": Found and updating task " + task.getTaskName());
                            subject.setValue(task);
                            break;
                        }
                    }
                }
            };
            
            repository.getTasks().observe(taskObserver);
            
            return subject;
        }
        
        @Override
        public Subject<List<Task>> findAll() {
            Log.d(TAG, "TaskRepository #" + repositoryId + ": findAll() called");
            return repository.getTasks();
        }
        
        @Override
        public void save(Task task) {
            Log.d(TAG, "TaskRepository #" + repositoryId + ": Saving task " + 
                  (task != null ? task.getTaskName() : "null"));
            if (task.getTaskId() != null) {
                repository.updateTask(task);
            } else {
                repository.addTask(task);
            }
        }
    }
    
    // Implementation of RoutineRepository that uses HabitizerRepository
    private class HabitizerRoutineRepository extends RoutineRepository {
        private final int repositoryId;
        private boolean isCleaned = false;
        // Add lock for synchronization
        private final Object routinesLock = new Object();
        
        public HabitizerRoutineRepository() {
            super(new InMemoryDataSource()); // This won't be used
            repositoryId = observerCount.incrementAndGet();
            Log.d(TAG, "Created HabitizerRoutineRepository #" + repositoryId);
        }
        
        @Override
        public int count() {
            List<Routine> routines = repository.getRoutines().getValue();
            int count = routines != null ? routines.size() : 0;
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": count() returning " + count);
            return count;
        }
        
        @Override
        public Subject<Routine> find(int id) {
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": Looking for routine with ID " + id);
            // Create a subject that observes the repository's routine
            PlainMutableSubject<Routine> subject = new PlainMutableSubject<>();
            
            // Initialize with current value if available
            List<Routine> routines = repository.getRoutines().getValue();
            if (routines != null) {
                for (Routine routine : routines) {
                    if (routine.getRoutineId() == id) {
                        Log.d(TAG, "RoutineRepository #" + repositoryId + ": Found routine " + routine.getRoutineName());
                        subject.setValue(routine);
                        break;
                    }
                }
            }
            
            // Observe repository changes
            int observerId = observerCount.incrementAndGet();
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": Creating routine observer #" + observerId + " for routine ID " + id);
            
            Observer<List<Routine>> routineObserver = routineList -> {
                Log.d(TAG, "Routine observer #" + observerId + " triggered with " + (routineList != null ? routineList.size() : 0) + " routines");
                if (routineList != null) {
                    for (Routine routine : routineList) {
                        if (routine.getRoutineId() == id) {
                            Log.d(TAG, "Routine observer #" + observerId + ": Found and updating routine " + routine.getRoutineName());
                            subject.setValue(routine);
                            break;
                        }
                    }
                }
            };
            
            repository.getRoutines().observe(routineObserver);
            
            return subject;
        }
        
        @Override
        public Routine getRoutine(int id) {
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": getRoutine(" + id + ") called");
            List<Routine> routines = repository.getRoutines().getValue();
            if (routines != null) {
                for (Routine routine : routines) {
                    if (routine.getRoutineId() == id) {
                        Log.d(TAG, "RoutineRepository #" + repositoryId + ": Found routine " + routine.getRoutineName());
                        return routine;
                    }
                }
            }
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": Could not find routine with ID " + id);
            return null;
        }
        
        // Subject returned to clients for findAll, which might be subscribed to multiple times
        private final PlainMutableSubject<List<Routine>> allRoutinesSubject = new PlainMutableSubject<>(new ArrayList<>());
        private Observer<List<Routine>> repositoryRoutineObserver = null;
        
        @Override
        public Subject<List<Routine>> findAll() {
            synchronized (routinesLock) {
                Log.d(TAG, "RoutineRepository #" + repositoryId + ": findAll() called, hasObservers=" + allRoutinesSubject.hasObservers());
                
                // Only observe the repository once, no matter how many times findAll() is called
                if (repositoryRoutineObserver == null) {
                    int observerId = observerCount.incrementAndGet();
                    Log.d(TAG, "RoutineRepository #" + repositoryId + ": Creating shared routine observer #" + observerId);
                    
                    repositoryRoutineObserver = routineList -> {
                        synchronized (routinesLock) {
                            Log.d(TAG, "Shared routine observer #" + observerId + " triggered with " + 
                                  (routineList != null ? routineList.size() : 0) + " routines, has " + 
                                  allRoutinesSubject.getObservers().size() + " subscribers");
                            
                            if (routineList != null) {
                                // Check for duplicates in the incoming list
                                Map<Integer, Routine> uniqueRoutineMap = new HashMap<>();
                                for (Routine r : routineList) {
                                    uniqueRoutineMap.put(r.getRoutineId(), r);
                                }
                                
                                List<Routine> deduplicatedList = new ArrayList<>(uniqueRoutineMap.values());
                                
                                if (deduplicatedList.size() < routineList.size()) {
                                    Log.w(TAG, "Found and removed " + (routineList.size() - deduplicatedList.size()) + 
                                          " duplicate routines in repository data");
                                }
                                
                                // Create a copy to avoid reference issues
                                List<Routine> copyList = new ArrayList<>(deduplicatedList);
                                Log.d(TAG, "Setting " + copyList.size() + " routines on allRoutinesSubject");
                                allRoutinesSubject.setValue(copyList);
                            } else {
                                Log.d(TAG, "Setting empty list on allRoutinesSubject (null value received)");
                                allRoutinesSubject.setValue(new ArrayList<>());
                            }
                        }
                    };
                    
                    // Initialize with current values
                    List<Routine> currentRoutines = repository.getRoutines().getValue();
                    if (currentRoutines != null) {
                        // Check for duplicates in the initial list
                        Map<Integer, Routine> uniqueRoutineMap = new HashMap<>();
                        for (Routine r : currentRoutines) {
                            uniqueRoutineMap.put(r.getRoutineId(), r);
                        }
                        
                        List<Routine> deduplicatedList = new ArrayList<>(uniqueRoutineMap.values());
                        
                        if (deduplicatedList.size() < currentRoutines.size()) {
                            Log.w(TAG, "Found and removed " + (currentRoutines.size() - deduplicatedList.size()) + 
                                  " duplicate routines in initial data");
                        }
                        
                        // Create a copy to avoid reference issues
                        List<Routine> copyList = new ArrayList<>(deduplicatedList);
                        Log.d(TAG, "Initially setting " + copyList.size() + " routines on allRoutinesSubject");
                        allRoutinesSubject.setValue(copyList);
                    } else {
                        Log.d(TAG, "Initially setting 0 routines on allRoutinesSubject");
                        allRoutinesSubject.setValue(new ArrayList<>());
                    }
                    
                    // Subscribe the observer to the repository
                    repository.getRoutines().observe(repositoryRoutineObserver);
                }
                
                return allRoutinesSubject;
            }
        }
        
        @Override
        public void save(Routine routine) {
            synchronized (routinesLock) {
                Log.d(TAG, "RoutineRepository #" + repositoryId + ": Saving routine " + 
                      (routine != null ? routine.getRoutineName() : "null"));
                      
                // Add retry logic with explicit transaction
                int maxRetries = 3;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    try {
                        if (routine.getRoutineId() == 0) {
                            // For new routines, avoid ID conflicts by explicitly setting a unique ID
                            // Get existing routines to find max ID
                            List<Routine> existingRoutines = repository.getRoutines().getValue();
                            int maxId = 0;
                            if (existingRoutines != null) {
                                for (Routine r : existingRoutines) {
                                    if (r.getRoutineId() > maxId) {
                                        maxId = r.getRoutineId();
                                    }
                                }
                            }
                            // Create a new routine with the new ID since Routine doesn't have a setter
                            Routine newRoutine = new Routine(maxId + 1, routine.getRoutineName());
                            // Copy over tasks
                            for (Task task : routine.getTasks()) {
                                newRoutine.addTask(task);
                            }
                            routine = newRoutine;
                            Log.d(TAG, "Generated new routine ID: " + routine.getRoutineId());
                        }
                        
                        // Add or update routine atomically
                        repository.addRoutine(routine);
                        
                        // Wait a moment to ensure persistence on Windows
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Sleep interrupted during routine save", e);
                        }
                        
                        Log.d(TAG, "Successfully saved routine " + routine.getRoutineName() + " with ID " + routine.getRoutineId());
                        break; // Success, exit loop
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving routine (attempt " + (attempt + 1) + "/" + maxRetries + ")", e);
                        if (attempt == maxRetries - 1) {
                            Log.e(TAG, "Failed to save routine after " + maxRetries + " attempts");
                        } else {
                            try {
                                Thread.sleep(50); // Wait before retry
                            } catch (InterruptedException ie) {
                                Log.e(TAG, "Sleep interrupted", ie);
                            }
                        }
                    }
                }
            }
        }
        
        public void cleanUp() {
            synchronized (routinesLock) {
                if (!isCleaned) {
                    Log.d(TAG, "RoutineRepository #" + repositoryId + ": cleanUp() called");
                    isCleaned = true;
                }
            }
        }
    }
    
    // Adapter methods for MainViewModel compatibility
    public TaskRepository getTaskRepository() {
        return taskRepository;
    }
    
    public RoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    /**
     * Initialize the database with default data if it's empty
     */
    private void initializeDefaultDataIfNeeded() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting default data initialization with platform-specific optimizations");
                Log.d(TAG, "Running on platform: " + System.getProperty("os.name"));
                
                // Ensure we have a clean start by waiting for repository to load
                try {
                    Thread.sleep(300); // Increase wait time to ensure repository is ready
                    Log.d(TAG, "Initial wait completed, proceeding with routine initialization");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted", e);
                }

                // Get existing routines
                List<Routine> existingRoutines = repository.getRoutines().getValue();
                Log.d(TAG, "Checking existing routines during initialization");
                if (existingRoutines != null) {
                    Log.d(TAG, "Found " + existingRoutines.size() + " existing routines in database");
                    for (Routine r : existingRoutines) {
                        Log.d(TAG, "  Existing Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
                    }
                } else {
                    Log.d(TAG, "***WARNING: Existing routines is NULL - this indicates a repository initialization problem");
                }

                // Map to track which default routines we need to create
                Map<String, Boolean> routinesExist = new HashMap<>();
                routinesExist.put("Morning", false);
                routinesExist.put("Evening", false);

                // Check which default routines already exist
                if (existingRoutines != null && !existingRoutines.isEmpty()) {
                    Log.d(TAG, "Checking for default routines among existing routines");
                    for (Routine r : existingRoutines) {
                        String routineName = r.getRoutineName();
                        if ("Morning".equals(routineName) || "Evening".equals(routineName)) {
                            routinesExist.put(routineName, true);
                            Log.d(TAG, "  Found existing default routine: " + routineName + " (ID: " + r.getRoutineId() + ")");
                        }
                    }
                }

                // On Windows, add extra check with a short delay to ensure routines are properly loaded
                String osName = System.getProperty("os.name", "unknown").toLowerCase();
                Log.d(TAG, "Detailed OS information: " + osName);
                
                // Add delay for all platforms to ensure DB is ready
                try {
                    Log.d(TAG, "Waiting to ensure database is ready...");
                    Thread.sleep(500);
                    // Refresh routine list after delay
                    existingRoutines = repository.getRoutines().getValue();
                    if (existingRoutines != null) {
                        Log.d(TAG, "After delay: Found " + existingRoutines.size() + " routines");
                        for (Routine r : existingRoutines) {
                            String routineName = r.getRoutineName();
                            if ("Morning".equals(routineName) || "Evening".equals(routineName)) {
                                routinesExist.put(routineName, true);
                                Log.d(TAG, "  After delay: Found routine: " + routineName);
                            }
                        }
                    } else {
                        Log.d(TAG, "***WARNING: Routines list is still NULL after delay");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted during verification", e);
                }

                // Detect if we're running in test mode by checking the package name
                boolean isTestMode = getPackageName().contains("test");

                // In test mode, always ensure routines are recreated
                if (isTestMode) {
                    Log.d(TAG, "Test mode detected, forcing routine recreation");
                    routinesExist.put("Morning", false);
                    routinesExist.put("Evening", false);
                }

                Log.d(TAG, "Need to create Morning routine: " + !routinesExist.get("Morning"));
                Log.d(TAG, "Need to create Evening routine: " + !routinesExist.get("Evening"));

                // Create missing routines with synchronized access
                synchronized (routineRepository) {
                    if (!routinesExist.get("Morning")) {
                        // Create morning routine with default ID
                        Routine morningRoutine = new Routine(0, "Morning");

                        // Add tasks to the routine
                        Log.d(TAG, "Adding tasks to Morning routine");
                        for (Task task : DEFAULT_MORNING_TASKS) {
                            morningRoutine.addTask(task);
                        }

                        // Log task count
                        Log.d(TAG, "Morning routine has " + morningRoutine.getTasks().size() + " tasks");

                        // Add the morning routine
                        Log.d(TAG, "Adding morning routine with ID: " + morningRoutine.getRoutineId());
                        
                        // Add with retry logic
                        boolean morningSuccess = false;
                        for (int attempt = 1; attempt <= 3; attempt++) {
                            try {
                                repository.addRoutine(morningRoutine);
                                Log.d(TAG, "Morning routine added successfully on attempt " + attempt);
                                morningSuccess = true;
                                break;
                            } catch (Exception e) {
                                Log.e(TAG, "Error adding morning routine (attempt " + attempt + "/3)", e);
                                if (attempt < 3) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException ie) {
                                        Log.e(TAG, "Sleep interrupted", ie);
                                    }
                                }
                            }
                        }

                        if (!morningSuccess) {
                            Log.e(TAG, "CRITICAL ERROR: Failed to add Morning routine after multiple attempts");
                        }

                        // Wait a bit to ensure the routine is fully saved
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Sleep interrupted", e);
                        }
                    } else {
                        Log.d(TAG, "Morning routine already exists, skipping creation.");
                    }

                    if (!routinesExist.get("Evening")) {
                        // Create evening routine with default ID
                        Routine eveningRoutine = new Routine(1, "Evening");

                        // Add tasks to the routine
                        Log.d(TAG, "Adding tasks to Evening routine");
                        for (Task task : DEFAULT_EVENING_TASKS) {
                            eveningRoutine.addTask(task);
                        }

                        // Log task count
                        Log.d(TAG, "Evening routine has " + eveningRoutine.getTasks().size() + " tasks");

                        // Add the evening routine
                        Log.d(TAG, "Adding evening routine with ID: " + eveningRoutine.getRoutineId());
                        
                        // Add with retry logic
                        boolean eveningSuccess = false;
                        for (int attempt = 1; attempt <= 3; attempt++) {
                            try {
                                repository.addRoutine(eveningRoutine);
                                Log.d(TAG, "Evening routine added successfully on attempt " + attempt);
                                eveningSuccess = true;
                                break;
                            } catch (Exception e) {
                                Log.e(TAG, "Error adding evening routine (attempt " + attempt + "/3)", e);
                                if (attempt < 3) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException ie) {
                                        Log.e(TAG, "Sleep interrupted", ie);
                                    }
                                }
                            }
                        }
                        
                        if (!eveningSuccess) {
                            Log.e(TAG, "CRITICAL ERROR: Failed to add Evening routine after multiple attempts");
                        }

                        // Wait a bit to ensure the routine is fully saved
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Sleep interrupted", e);
                        }
                    } else {
                        Log.d(TAG, "Evening routine already exists, skipping creation.");
                    }
                }

                Log.d(TAG, "Default data initialization complete");

                // Force refresh from repository to ensure routines are loaded
                repository.refreshRoutines();
                
                // Double check that routines were added
                try {
                    Thread.sleep(1000); // Wait longer to ensure repository refresh completes
                    List<Routine> checkRoutines = repository.getRoutines().getValue();
                    if (checkRoutines != null) {
                        Log.d(TAG, "FINAL Verification: Found " + checkRoutines.size() + " routines after initialization");
                        for (Routine r : checkRoutines) {
                            Log.d(TAG, "  Routine ID: " + r.getRoutineId() + ", Name: " + r.getRoutineName() +
                                    ", Tasks: " + r.getTasks().size());
                        }
                        
                        // Check specifically for Morning and Evening routines
                        boolean foundMorning = false;
                        boolean foundEvening = false;
                        for (Routine r : checkRoutines) {
                            if ("Morning".equals(r.getRoutineName())) foundMorning = true;
                            if ("Evening".equals(r.getRoutineName())) foundEvening = true;
                        }
                        
                        if (!foundMorning) {
                            Log.e(TAG, "CRITICAL ERROR: Morning routine is missing after initialization");
                        }
                        if (!foundEvening) {
                            Log.e(TAG, "CRITICAL ERROR: Evening routine is missing after initialization");
                        }
                        
                        if (foundMorning && foundEvening) {
                            Log.d(TAG, "SUCCESS: Both Morning and Evening routines found after initialization");
                        }
                    } else {
                        Log.e(TAG, "CRITICAL ERROR: Verification failed - No routines found after initialization");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted during verification", e);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error initializing default data", e);
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ThreeTenABP library to support Java 8 date-time API
        try {
            Class.forName("com.jakewharton.threetenabp.AndroidThreeTen");
            com.jakewharton.threetenabp.AndroidThreeTen.init(this);
            Log.d(TAG, "AndroidThreeTen initialized successfully");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "AndroidThreeTen not found, skipping initialization", e);
        }
        
        // Initialize repository
        repository = HabitizerRepository.getInstance(this);
        
        // Initialize compatibility repositories
        this.taskRepository = new HabitizerTaskRepository();
        this.routineRepository = new HabitizerRoutineRepository();
        
        // Initialize default data if needed
        initializeDefaultDataIfNeeded();
        
        // Clean up any duplicate routines after initialization
        cleanupDuplicateRoutines();
        
        Log.d(TAG, "Application initialized with Room database");
    }

    /**
     * Cleans up any duplicate routines that may have been created during initialization.
     * This helps ensure consistent behavior across platforms.
     */
    private void cleanupDuplicateRoutines() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Wait for initialization to complete
                try {
                    Thread.sleep(1500);
                    Log.d(TAG, "Starting duplicate routine cleanup after initialization wait");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted", e);
                }
                
                List<Routine> routines = repository.getRoutines().getValue();
                
                if (routines == null || routines.isEmpty()) {
                    Log.e(TAG, "CRITICAL ERROR: No routines found during cleanup - skipping cleanup to avoid data loss");
                    return;
                }
                
                Log.d(TAG, "Found " + routines.size() + " routines to check for duplicates");
                
                // Map to store unique routines by name
                Map<String, Routine> uniqueRoutinesByName = new HashMap<>();
                List<Routine> duplicatesToRemove = new ArrayList<>();
                
                // First pass - identify duplicates and keep routines with most tasks
                for (Routine routine : routines) {
                    String name = routine.getRoutineName();
                    if (uniqueRoutinesByName.containsKey(name)) {
                        Routine existing = uniqueRoutinesByName.get(name);
                        // Keep the routine with the most tasks
                        if (routine.getTasks().size() > existing.getTasks().size()) {
                            duplicatesToRemove.add(existing);
                            uniqueRoutinesByName.put(name, routine);
                            Log.d(TAG, "Replacing routine " + name + " with ID " + existing.getRoutineId() + 
                                  " with one that has more tasks (ID: " + routine.getRoutineId() + ")");
                        } else {
                            duplicatesToRemove.add(routine);
                            Log.d(TAG, "Marking duplicate routine " + name + " with ID " + routine.getRoutineId() + " for removal");
                        }
                    } else {
                        uniqueRoutinesByName.put(name, routine);
                        Log.d(TAG, "Keeping unique routine " + name + " with ID " + routine.getRoutineId());
                    }
                }
                
                // Safety check - ensure we're not removing all routines
                if (duplicatesToRemove.size() >= routines.size()) {
                    Log.e(TAG, "SAFETY VIOLATION: Attempted to remove all routines during cleanup! Aborting duplicate cleanup.");
                    return;
                }
                
                // Log what we're keeping
                Log.d(TAG, "After cleanup, will keep " + uniqueRoutinesByName.size() + " unique routines:");
                for (Routine r : uniqueRoutinesByName.values()) {
                    Log.d(TAG, "  Keeping: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + 
                          ") with " + r.getTasks().size() + " tasks");
                }
                
                // Remove duplicates only if we found some and we're still keeping some routines
                if (!duplicatesToRemove.isEmpty() && uniqueRoutinesByName.size() > 0) {
                    Log.d(TAG, "Cleaning up " + duplicatesToRemove.size() + " duplicate routines");
                    
                    // Instead of updating/removing, we'll just update the repository with our filtered list
                    List<Routine> cleanedRoutines = new ArrayList<>(uniqueRoutinesByName.values());
                    
                    // Force update repository with cleaned list
                    try {
                        // Update repository with cleaned list
                        repository.setRoutines(cleanedRoutines);
                        Log.d(TAG, "Successfully updated repository with " + cleanedRoutines.size() + " cleaned routines");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating repository with cleaned routines", e);
                    }
                    
                    // Final verification
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                    
                    List<Routine> finalRoutines = repository.getRoutines().getValue();
                    if (finalRoutines != null) {
                        Log.d(TAG, "After cleanup: " + finalRoutines.size() + " routines remain");
                        for (Routine r : finalRoutines) {
                            Log.d(TAG, "  Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + 
                                  ") with " + r.getTasks().size() + " tasks");
                        }
                    }
                } else {
                    Log.d(TAG, "No duplicate routines found during cleanup, or no routines would remain after cleanup");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during duplicate routine cleanup", e);
            }
        });
    }

    /**
     * Verify that routines are loaded correctly with all their tasks
     */
    private void verifyRoutinesLoaded() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Log.d(TAG, "Verifying routines and tasks are properly loaded...");
            try {
                // First refresh routines to ensure we have the latest data
                repository.refreshRoutines();
                
                // Wait for refresh to complete
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted during verification", e);
                }
                
                // Check task associations directly in the database
                verifyAndFixTaskAssociationsInDatabase();
                
                // Wait for database operations to complete
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted during verification", e);
                }
                
                // Refresh again after fixing
                repository.refreshRoutines();
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted during verification", e);
                }
                
                // Get all routines from the repository using the findAll method
                Subject<List<Routine>> routinesSubject = routineRepository.findAll();
                List<Routine> routines = routinesSubject.getValue();
                
                if (routines == null || routines.isEmpty()) {
                    Log.e(TAG, "No routines found in repository! This is unexpected.");
                    return;
                }

                Log.d(TAG, "Found " + routines.size() + " routines in repository");
                
                // Check each routine for tasks
                for (Routine routine : routines) {
                    String routineName = routine.getRoutineName();
                    List<Task> tasks = routine.getTasks();
                    
                    Log.d(TAG, "Routine '" + routineName + "' has " + tasks.size() + " tasks");
                    
                    if (tasks.isEmpty()) {
                        Log.e(TAG, "ERROR: Routine '" + routineName + "' has no tasks! This indicates a loading issue.");
                        
                        // If Morning or Evening routine is missing tasks, let's fix it
                        if ("Morning".equals(routineName) || "Evening".equals(routineName)) {
                            Log.d(TAG, "Attempting to fix missing tasks for " + routineName + " routine");
                            fixRoutineWithDefaultTasks(routine);
                        }
                    } else {
                        // Log all tasks for this routine
                        for (Task task : tasks) {
                            Log.d(TAG, "  - Task: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")");
                        }
                    }
                }
                
                Log.d(TAG, "Routine verification complete");
            } catch (Exception e) {
                Log.e(TAG, "Error during routine verification", e);
            }
        });
    }
    
    /**
     * Verify and fix task associations directly in the database
     */
    private void verifyAndFixTaskAssociationsInDatabase() {
        try {
            Log.d(TAG, "Checking and fixing task associations directly in the database");
            
            // Get a reference to the database
            AppDatabase db = repository.getDatabase();
            
            // Check how many associations exist in the cross-reference table
            List<RoutineTaskCrossRef> crossRefs = db.routineDao().getAllTaskRelationshipsOrdered();
            Log.d(TAG, "Found " + (crossRefs != null ? crossRefs.size() : 0) + " task-routine associations in database");
            
            // Get routines
            List<RoutineEntity> routineEntities = db.routineDao().findAll();
            if (routineEntities == null || routineEntities.isEmpty()) {
                Log.e(TAG, "No routines found in database");
                return;
            }
            Log.d(TAG, "Found " + routineEntities.size() + " routines in database");
            
            // Check for Morning and Evening routines
            RoutineEntity morningRoutine = null;
            RoutineEntity eveningRoutine = null;
            
            for (RoutineEntity routine : routineEntities) {
                if ("Morning".equals(routine.getRoutineName())) {
                    morningRoutine = routine;
                    Log.d(TAG, "Found Morning routine in database with ID: " + routine.getId());
                } else if ("Evening".equals(routine.getRoutineName())) {
                    eveningRoutine = routine;
                    Log.d(TAG, "Found Evening routine in database with ID: " + routine.getId());
                }
            }
            
            // Get all tasks
            List<TaskEntity> allTasks = db.taskDao().findAll();
            if (allTasks == null || allTasks.isEmpty()) {
                Log.d(TAG, "No tasks found in database. Creating default tasks...");
                
                // Create default tasks
                List<TaskEntity> tasksToAdd = new ArrayList<>();
                
                // Add default morning tasks
                for (Task task : DEFAULT_MORNING_TASKS) {
                    TaskEntity taskEntity = TaskEntity.fromTask(task);
                    tasksToAdd.add(taskEntity);
                }
                
                // Add default evening tasks
                for (Task task : DEFAULT_EVENING_TASKS) {
                    TaskEntity taskEntity = TaskEntity.fromTask(task);
                    tasksToAdd.add(taskEntity);
                }
                
                // Insert tasks into database
                db.taskDao().insertAll(tasksToAdd);
                Log.d(TAG, "Added " + tasksToAdd.size() + " default tasks to database");
                
                // Refresh the task list
                allTasks = db.taskDao().findAll();
            }
            
            Log.d(TAG, "Found " + allTasks.size() + " tasks in database");
            
            // Maps to store tasks by name for quick lookup
            Map<String, TaskEntity> tasksByName = new HashMap<>();
            for (TaskEntity task : allTasks) {
                tasksByName.put(task.getTaskName(), task);
            }
            
            // Fix Morning routine if needed
            if (morningRoutine != null) {
                // Check if Morning routine has associations
                List<RoutineTaskCrossRef> morningRefs = db.routineDao().getTaskPositions(morningRoutine.getId());
                if (morningRefs == null || morningRefs.isEmpty()) {
                    Log.d(TAG, "Morning routine has no task associations. Adding default tasks...");
                    
                    // Delete any existing associations to be safe
                    db.routineDao().deleteRoutineTaskCrossRefs(morningRoutine.getId());
                    
                    // Add associations for default morning tasks
                    for (int i = 0; i < DEFAULT_MORNING_TASKS.size(); i++) {
                        Task task = DEFAULT_MORNING_TASKS.get(i);
                        TaskEntity taskEntity = tasksByName.get(task.getTaskName());
                        
                        if (taskEntity != null) {
                            RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                                morningRoutine.getId(),
                                taskEntity.getId(),
                                i
                            );
                            db.routineDao().insertRoutineTaskCrossRef(crossRef);
                            Log.d(TAG, "Added association between Morning routine and task: " + taskEntity.getTaskName());
                        } else {
                            Log.e(TAG, "Could not find task entity for: " + task.getTaskName());
                        }
                    }
                } else {
                    Log.d(TAG, "Morning routine already has " + morningRefs.size() + " task associations");
                }
            }
            
            // Fix Evening routine if needed
            if (eveningRoutine != null) {
                // Check if Evening routine has associations
                List<RoutineTaskCrossRef> eveningRefs = db.routineDao().getTaskPositions(eveningRoutine.getId());
                if (eveningRefs == null || eveningRefs.isEmpty()) {
                    Log.d(TAG, "Evening routine has no task associations. Adding default tasks...");
                    
                    // Delete any existing associations to be safe
                    db.routineDao().deleteRoutineTaskCrossRefs(eveningRoutine.getId());
                    
                    // Add associations for default evening tasks
                    for (int i = 0; i < DEFAULT_EVENING_TASKS.size(); i++) {
                        Task task = DEFAULT_EVENING_TASKS.get(i);
                        TaskEntity taskEntity = tasksByName.get(task.getTaskName());
                        
                        if (taskEntity != null) {
                            RoutineTaskCrossRef crossRef = new RoutineTaskCrossRef(
                                eveningRoutine.getId(),
                                taskEntity.getId(),
                                i
                            );
                            db.routineDao().insertRoutineTaskCrossRef(crossRef);
                            Log.d(TAG, "Added association between Evening routine and task: " + taskEntity.getTaskName());
                        } else {
                            Log.e(TAG, "Could not find task entity for: " + task.getTaskName());
                        }
                    }
                } else {
                    Log.d(TAG, "Evening routine already has " + eveningRefs.size() + " task associations");
                }
            }
            
            Log.d(TAG, "Task association verification and fixing complete");
        } catch (Exception e) {
            Log.e(TAG, "Error verifying and fixing task associations", e);
        }
    }
    
    /**
     * Fix a routine by adding default tasks to it
     * @param routine The routine to fix
     */
    private void fixRoutineWithDefaultTasks(Routine routine) {
        try {
            String routineName = routine.getRoutineName();
            Log.d(TAG, "Fixing routine: " + routineName);
            
            // Add appropriate tasks based on routine name
            if ("Morning".equals(routineName)) {
                // Add default morning tasks
                for (Task task : DEFAULT_MORNING_TASKS) {
                    routine.addTask(task);
                }
                Log.d(TAG, "Added " + DEFAULT_MORNING_TASKS.size() + " default tasks to Morning routine");
            } else if ("Evening".equals(routineName)) {
                // Add default evening tasks
                for (Task task : DEFAULT_EVENING_TASKS) {
                    routine.addTask(task);
                }
                Log.d(TAG, "Added " + DEFAULT_EVENING_TASKS.size() + " default tasks to Evening routine");
            }
            
            // Update the routine in the repository to persist the changes
            repository.updateRoutine(routine);
            Log.d(TAG, "Updated " + routineName + " routine with default tasks in repository");
            
            // Wait for a moment to ensure the update is processed
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep interrupted", e);
            }
            
            // Refresh routines to ensure changes are visible
            repository.refreshRoutines();
        } catch (Exception e) {
            Log.e(TAG, "Error fixing routine with default tasks", e);
        }
    }
}
