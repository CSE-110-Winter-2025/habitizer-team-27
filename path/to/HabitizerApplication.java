package edu.ucsd.cse110.habitizer.app;

import android.app.Application;
import android.util.Log;

import androidx.room.Room;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import edu.ucsd.cse110.habitizer.room.AppDatabase;
import edu.ucsd.cse110.habitizer.room.HabitizerRoutine;
import edu.ucsd.cse110.habitizer.room.HabitizerTask;
import edu.ucsd.cse110.habitizer.room.RoutineDao;
import edu.ucsd.cse110.habitizer.room.TaskDao;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class HabitizerApplication extends Application {
    private static final String TAG = "HabitizerApplication";
    private static HabitizerApplication singleton = null;
    private AppDatabase db;
    private HabitizerTaskRepository taskRepository;
    private HabitizerRoutineRepository routineRepository;

    public HabitizerApplication() {
        super();
        singleton = this;
    }

    public static HabitizerApplication getSingleton() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        Log.d(TAG, "AndroidThreeTen initialized successfully");

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "habitizer-db")
                .build();
        Log.d(TAG, "Application initialized with Room database");

        taskRepository = new HabitizerTaskRepository(db.taskDao());
        Log.d(TAG, "Created HabitizerTaskRepository #1");
        routineRepository = new HabitizerRoutineRepository(db.routineDao());
        Log.d(TAG, "Created HabitizerRoutineRepository #2");

        initializeDefaultDataIfNeeded();
    }

    public void initializeDefaultDataIfNeeded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(TAG, "Checking existing routines during initialization");
            routineRepository.hasAnyRoutines()
                    .subscribeOn(Schedulers.io())
                    .subscribe(hasRoutines -> {
                        Log.d(TAG, "Found " + (hasRoutines ? "existing" : "no existing") + " routines in database");
                        if (!hasRoutines) {
                            Log.d(TAG, "Need to create default routines. Checking tasks...");
                            taskRepository.hasAnyTasks()
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(hasTasks -> {
                                        if (!hasTasks) {
                                            Log.d(TAG, "No existing tasks, adding default tasks and routines");
                                            addDefaultTasksAndRoutines();
                                        } else {
                                            Log.d(TAG, "Existing tasks found, only adding default routines");
                                            addDefaultRoutines();
                                        }
                                    }, throwable -> {
                                        Log.e(TAG, "Error checking for existing tasks", throwable);
                                    });
                        } else {
                            Log.d(TAG, "Default data initialization skipped, routines already exist");
                            verifyRoutinesLoaded();
                        }
                    }, throwable -> {
                        Log.e(TAG, "Error checking for existing routines", throwable);
                    });
        });
    }


    private void addDefaultTasksAndRoutines() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(TAG, "Adding default tasks and routines...");
            List<HabitizerTask> morningTasks = Arrays.asList(
                    new HabitizerTask("Shower"),
                    new HabitizerTask("Brush teeth"),
                    new HabitizerTask("Dress"),
                    new HabitizerTask("Make coffee"),
                    new HabitizerTask("Make lunch"),
                    new HabitizerTask("Dinner prep"),
                    new HabitizerTask("Pack bag")
            );

            List<HabitizerTask> eveningTasks = Arrays.asList(
                    new HabitizerTask("Charge devices"),
                    new HabitizerTask("Prepare dinner"),
                    new HabitizerTask("Eat dinner"),
                    new HabitizerTask("Wash dishes"),
                    new HabitizerTask("Pack bag"),
                    new HabitizerTask("Homework")
            );

            Completable.fromAction(() -> {
                Log.d(TAG, "Starting task additions...");
                TaskDao taskDao = db.taskDao();
                RoutineDao routineDao = db.routineDao();

                Log.d(TAG, "Checking if Morning routine exists...");
                if (routineDao.findByName("Morning") == null) {
                    Log.d(TAG, "Morning routine is missing, adding morning tasks");
                    HabitizerRoutine morningRoutine = new HabitizerRoutine("Morning");
                    long morningRoutineId = routineDao.insert(morningRoutine);
                    Log.d(TAG, "Added morning routine with ID: " + morningRoutineId);

                    Log.d(TAG, "Adding tasks to Morning routine");
                    for (HabitizerTask task : morningTasks) {
                        long taskId = taskDao.insert(task);
                        Log.d(TAG, "Added morning task: " + task.getName() + " with ID: " + taskId);
                        routineDao.addTaskToRoutine(morningRoutineId, taskId);
                    }
                    Log.d(TAG, "Morning routine has " + morningTasks.size() + " tasks");

                } else {
                    Log.d(TAG, "Morning routine already exists, skipping creation");
                }

                Log.d(TAG, "Checking if Evening routine exists...");
                if (routineDao.findByName("Evening") == null) {
                    Log.d(TAG, "Evening routine is missing, adding evening tasks");
                    HabitizerRoutine eveningRoutine = new HabitizerRoutine("Evening");
                    long eveningRoutineId = routineDao.insert(eveningRoutine);
                    Log.d(TAG, "Added evening routine with ID: " + eveningRoutineId);

                    Log.d(TAG, "Adding tasks to Evening routine");
                    for (HabitizerTask task : eveningTasks) {
                        long taskId = taskDao.insert(task);
                        Log.d(TAG, "Added evening task: " + task.getName() + " with ID: " + taskId);
                        routineDao.addTaskToRoutine(eveningRoutineId, taskId);
                    }
                    Log.d(TAG, "Evening routine has " + eveningTasks.size() + " tasks");
                } else {
                    Log.d(TAG, "Evening routine already exists, skipping creation");
                }
                Log.d(TAG, "Waiting for task additions to complete...");

            }).subscribeOn(Schedulers.io()).subscribe(() -> {
                Log.d(TAG, "Default data initialization complete");
                verifyRoutinesLoaded();
            }, throwable -> {
                Log.e(TAG, "Error initializing default data", throwable);
            });
        });
    }

    private void addDefaultRoutines() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(TAG, "Adding default routines...");
            List<HabitizerTask> morningTasks = Arrays.asList(
                    new HabitizerTask("Shower"),
                    new HabitizerTask("Brush teeth"),
                    new HabitizerTask("Dress"),
                    new HabitizerTask("Make coffee"),
                    new HabitizerTask("Make lunch"),
                    new HabitizerTask("Dinner prep"),
                    new HabitizerTask("Pack bag")
            );

            List<HabitizerTask> eveningTasks = Arrays.asList(
                    new HabitizerTask("Charge devices"),
                    new HabitizerTask("Prepare dinner"),
                    new HabitizerTask("Eat dinner"),
                    new HabitizerTask("Wash dishes"),
                    new HabitizerTask("Pack bag"),
                    new HabitizerTask("Homework")
            );


            Completable.fromAction(() -> {
                RoutineDao routineDao = db.routineDao();
                TaskDao taskDao = db.taskDao();

                Log.d(TAG, "Checking if Morning routine exists...");
                if (routineDao.findByName("Morning") == null) {
                    Log.d(TAG, "Morning routine is missing, adding morning tasks");
                    HabitizerRoutine morningRoutine = new HabitizerRoutine("Morning");
                    long morningRoutineId = routineDao.insert(morningRoutine);
                    Log.d(TAG, "Added morning routine with ID: " + morningRoutineId);

                    Log.d(TAG, "Adding tasks to Morning routine");
                    for (HabitizerTask task : morningTasks) {
                        HabitizerTask existingTask = taskDao.findByName(task.getName());
                        long taskId;
                        if (existingTask == null) {
                            taskId = taskDao.insert(task);
                            Log.d(TAG, "Added morning task: " + task.getName() + " with ID: " + taskId);
                        } else {
                            taskId = existingTask.getId();
                            Log.d(TAG, "Using existing morning task: " + task.getName() + " with ID: " + taskId);
                        }
                        routineDao.addTaskToRoutine(morningRoutineId, taskId);
                    }
                    Log.d(TAG, "Morning routine has " + morningTasks.size() + " tasks");
                } else {
                    Log.d(TAG, "Morning routine already exists, skipping creation");
                }


                Log.d(TAG, "Checking if Evening routine exists...");
                if (routineDao.findByName("Evening") == null) {
                    Log.d(TAG, "Evening routine is missing, adding evening tasks");
                    HabitizerRoutine eveningRoutine = new HabitizerRoutine("Evening");
                    long eveningRoutineId = routineDao.insert(eveningRoutine);
                    Log.d(TAG, "Added evening routine with ID: " + eveningRoutineId);

                    Log.d(TAG, "Adding tasks to Evening routine");
                    for (HabitizerTask task : eveningTasks) {
                        HabitizerTask existingTask = taskDao.findByName(task.getName());
                        long taskId;
                        if (existingTask == null) {
                            taskId = taskDao.insert(task);
                            Log.d(TAG, "Added evening task: " + task.getName() + " with ID: " + taskId);
                        } else {
                            taskId = existingTask.getId();
                            Log.d(TAG, "Using existing evening task: " + task.getName() + " with ID: " + taskId);
                        }
                        routineDao.addTaskToRoutine(eveningRoutineId, taskId);
                    }
                    Log.d(TAG, "Evening routine has " + eveningTasks.size() + " tasks");
                } else {
                    Log.d(TAG, "Evening routine already exists, skipping creation");
                }
            }).subscribeOn(Schedulers.io()).subscribe(() -> {
                Log.d(TAG, "Default routines initialization complete");
                verifyRoutinesLoaded();
            }, throwable -> {
                Log.e(TAG, "Error initializing default routines", throwable);
            });
        });
    }

    private void verifyRoutinesLoaded() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(TAG, "Verifying routines loaded...");
            routineRepository.getAllRoutines()
                    .subscribeOn(Schedulers.io())
                    .subscribe(routines -> {
                        Log.d(TAG, "Verification: Found " + routines.size() + " routines after initialization");
                        int morningRoutineCount = 0;
                        int eveningRoutineCount = 0;
                        for (HabitizerRoutine routine : routines) {
                            Log.d(TAG, "   Routine ID: " + routine.getId() + ", Name: " + routine.getName() + ", Tasks: " + routine.getTaskIds().size());
                            if ("Morning".equals(routine.getName())) {
                                morningRoutineCount++;
                            } else if ("Evening".equals(routine.getName())) {
                                eveningRoutineCount++;
                            }
                        }

                        if (morningRoutineCount == 0) {
                            Log.w(TAG, "Verification Warning: Morning routine is missing after initialization");
                        }
                        if (eveningRoutineCount == 0) {
                            Log.w(TAG, "Verification Warning: Evening routine is missing after initialization");
                        }
                        if (morningRoutineCount > 1 || eveningRoutineCount > 1) {
                            Log.w(TAG, "Verification Warning: Duplicate routines found after initialization");
                        }


                    }, throwable -> {
                        Log.e(TAG, "Error verifying routines", throwable);
                    });
        });
    }


    public HabitizerTaskRepository getTaskRepository() {
        return taskRepository;
    }

    public HabitizerRoutineRepository getRoutineRepository() {
        return routineRepository;
    }

    public AppDatabase getDatabase() {
        return db;
    }


    /**
     * Repository class for routines, handles data access and observation.
     */
    public static class HabitizerRoutineRepository {
        private final RoutineDao routineDao;
        private final BehaviorSubject<List<HabitizerRoutine>> allRoutinesSubject = BehaviorSubject.create();
        private int observerCounter = 0;

        public HabitizerRoutineRepository(RoutineDao routineDao) {
            this.routineDao = routineDao;
            loadInitialData();
        }

        private void loadInitialData() {
            Log.d(TAG, "RoutineRepository: loadInitialData() called");
            findAll().subscribe(routines -> {
                Log.d(TAG, "RoutineRepository: Initial data loaded, setting " + routines.size() + " routines on allRoutinesSubject");
                allRoutinesSubject.onNext(routines);
            }, throwable -> {
                Log.e(TAG, "RoutineRepository: Error loading initial data", throwable);
            });
        }


        public Flowable<List<HabitizerRoutine>> getAllRoutines() {
            return allRoutinesSubject;
        }


        public Single<HabitizerRoutine> findById(long id) {
            Log.d(TAG, "RoutineRepository #" + System.identityHashCode(this) + ": findById(" + id + ") called");
            return routineDao.findById(id);
        }


        public Completable save(HabitizerRoutine routine) {
            Log.d(TAG, "RoutineRepository #" + System.identityHashCode(this) + ": save(" + routine.getName() + ") called");
            return Completable.fromAction(() -> {
                if (routine.getId() == 0) {
                    long id = routineDao.insert(routine);
                    routine.setId(id);
                    Log.d(TAG, "RoutineRepository: Inserted new routine with ID: " + id);
                } else {
                    routineDao.update(routine);
                    Log.d(TAG, "RoutineRepository: Updated existing routine with ID: " + routine.getId());
                }
                findAll().subscribe(updatedRoutines -> {
                    Log.d(TAG, "RoutineRepository: save() - Publishing updated routines after save operation");
                    allRoutinesSubject.onNext(updatedRoutines);
                }, throwable -> {
                    Log.e(TAG, "RoutineRepository: Error fetching all routines after save", throwable);
                });
            });
        }


        public Completable delete(HabitizerRoutine routine) {
            return Completable.fromAction(() -> {
                routineDao.delete(routine);
                findAll().subscribe(updatedRoutines -> {
                    allRoutinesSubject.onNext(updatedRoutines);
                });
            });
        }


        public Single<Boolean> hasAnyRoutines() {
            return routineDao.hasAnyRoutines();
        }


        private Flowable<List<HabitizerRoutine>> findAll() {
            boolean hasObservers = allRoutinesSubject.hasObservers();
            Log.d(TAG, "RoutineRepository #" + System.identityHashCode(this) + ": findAll() called, hasObservers=" + hasObservers);

            if (!hasObservers) {
                int observerId = ++observerCounter;
                Log.d(TAG, "RoutineRepository #" + System.identityHashCode(this) + ": Creating shared routine observer #" + observerId);
                BehaviorSubject<List<HabitizerRoutine>> sharedObserver = BehaviorSubject.create();

                routineDao.findAll().subscribe(routines -> {
                    Log.d(TAG, "Shared routine observer #" + observerId + " triggered with " + routines.size() + " routines, has " + sharedObserver.hasObservers() + " subscribers");
                    sharedObserver.onNext(routines);
                }, throwable -> {
                    Log.e(TAG, "Shared routine observer #" + observerId + " error", throwable);
                    sharedObserver.onError(throwable);
                });

                sharedObserver.subscribe(routines -> {
                    Log.d(TAG, "Setting " + routines.size() + " routines on allRoutinesSubject");
                    allRoutinesSubject.onNext(routines);
                }, throwable -> {
                    Log.e(TAG, "Error in shared routine observer subscription", throwable);
                });
            }

            return allRoutinesSubject;
        }
    }


    /**
     * Repository class for tasks, handles data access and observation.
     */
    public static class HabitizerTaskRepository {
        private final TaskDao taskDao;

        public HabitizerTaskRepository(TaskDao taskDao) {
            this.taskDao = taskDao;
        }


        public Single<HabitizerTask> findById(long id) {
            return taskDao.findById(id);
        }


        public Completable save(HabitizerTask task) {
            return Completable.fromAction(() -> {
                if (task.getId() == 0) {
                    long id = taskDao.insert(task);
                    task.setId(id);
                } else {
                    taskDao.update(task);
                }
            });
        }


        public Completable delete(HabitizerTask task) {
            return Completable.fromAction(() -> taskDao.delete(task));
        }


        public Single<Boolean> hasAnyTasks() {
            return taskDao.hasAnyTasks();
        }

        public Single<List<HabitizerTask>> findAllTasksByName(List<String> taskNames) {
            return taskDao.findAllTasksByName(taskNames);
        }

    }
} 