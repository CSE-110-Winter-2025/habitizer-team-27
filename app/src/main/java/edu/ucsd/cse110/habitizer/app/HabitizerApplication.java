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

import edu.ucsd.cse110.habitizer.app.data.db.HabitizerRepository;
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
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": findAll() called, hasObservers=" + allRoutinesSubject.hasObservers());
            
            // Only observe the repository once, no matter how many times findAll() is called
            if (repositoryRoutineObserver == null) {
                int observerId = observerCount.incrementAndGet();
                Log.d(TAG, "RoutineRepository #" + repositoryId + ": Creating shared routine observer #" + observerId);
                
                repositoryRoutineObserver = routineList -> {
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
                        Log.d(TAG, "Setting " + deduplicatedList.size() + " routines on allRoutinesSubject");
                        allRoutinesSubject.setValue(deduplicatedList);
                    } else {
                        Log.d(TAG, "Setting empty list on allRoutinesSubject (null value received)");
                        allRoutinesSubject.setValue(new ArrayList<>());
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
                              " duplicate routines in initial repository data");
                    }
                    
                    Log.d(TAG, "Initially setting " + deduplicatedList.size() + " routines on allRoutinesSubject");
                    allRoutinesSubject.setValue(deduplicatedList);
                }
                
                // Start observing repository
                repository.getRoutines().observe(repositoryRoutineObserver);
            }
            
            return allRoutinesSubject;
        }
        
        @Override
        public void save(Routine routine) {
            Log.d(TAG, "RoutineRepository #" + repositoryId + ": Saving routine " + 
                  (routine != null ? routine.getRoutineName() : "null"));
            if (routine.getRoutineId() != null && getRoutine(routine.getRoutineId()) != null) {
                repository.updateRoutine(routine);
            } else {
                repository.addRoutine(routine);
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
                // Get existing routines
                List<Routine> existingRoutines = repository.getRoutines().getValue();
                Log.d(TAG, "Checking existing routines during initialization");
                if (existingRoutines != null) {
                    Log.d(TAG, "Found " + existingRoutines.size() + " existing routines in database");
                    for (Routine r : existingRoutines) {
                        Log.d(TAG, "  Existing Routine: " + r.getRoutineName() + " (ID: " + r.getRoutineId() + ")");
                    }
                } else {
                    Log.d(TAG, "No existing routines found in database initially");
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

                // Detect if we're running in test mode by checking the package name
                boolean isTestMode = getPackageName().contains("test");

                // In test mode, always ensure routines are recreated
                if (isTestMode) {
                    Log.d(TAG, "Running in test mode. Ensuring test routines are available.");
                    routinesExist.put("Morning", false);
                    routinesExist.put("Evening", false);
                }

                // First add all default tasks to the database if we need to create any routines
                if (!routinesExist.get("Morning") || !routinesExist.get("Evening")) {
                    Log.d(TAG, "Need to create default routines. Checking tasks...");
                    // Add morning tasks if morning routine doesn't exist
                    if (!routinesExist.get("Morning")) {
                        Log.d(TAG, "Morning routine is missing, adding morning tasks");
                        for (Task task : DEFAULT_MORNING_TASKS) {
                            repository.addTask(task);
                            Log.d(TAG, "Added morning task: " + task.getTaskName() + " with ID: " + task.getTaskId());
                        }
                    }

                    // Add evening tasks if evening routine doesn't exist
                    if (!routinesExist.get("Evening")) {
                        Log.d(TAG, "Evening routine is missing, adding evening tasks");
                        for (Task task : DEFAULT_EVENING_TASKS) {
                            repository.addTask(task);
                            Log.d(TAG, "Added evening task: " + task.getTaskName() + " with ID: " + task.getTaskId());
                        }
                    }

                    // Wait a bit longer for tasks to be added
                    try {
                        Log.d(TAG, "Waiting for task additions to complete...");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                } else {
                    Log.d(TAG, "Default routines exist, no need to add tasks.");
                }


                // Create and add Morning routine if it doesn't exist
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
                    repository.addRoutine(morningRoutine);

                    // Wait a bit to ensure the routine is fully saved
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                } else {
                    Log.d(TAG, "Morning routine already exists, skipping creation.");
                }

                // Create and add Evening routine if it doesn't exist
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
                    repository.addRoutine(eveningRoutine);

                    // Wait a bit to ensure the routine is fully saved
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                } else {
                    Log.d(TAG, "Evening routine already exists, skipping creation.");
                }

                Log.d(TAG, "Default data initialization complete");

                // Double check that routines were added
                List<Routine> checkRoutines = repository.getRoutines().getValue();
                if (checkRoutines != null) {
                    Log.d(TAG, "Verification: Found " + checkRoutines.size() + " routines after initialization");
                    for (Routine r : checkRoutines) {
                        Log.d(TAG, "  Routine ID: " + r.getRoutineId() + ", Name: " + r.getRoutineName() +
                                ", Tasks: " + r.getTasks().size());
                    }
                } else {
                    Log.e(TAG, "Verification failed: No routines found after initialization");
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
        
        Log.d(TAG, "Application initialized with Room database");
    }
}
