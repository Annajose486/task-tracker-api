package com.symphonyai.tasktracker.repository;

import com.symphonyai.tasktracker.entity.Task;
import com.symphonyai.tasktracker.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByStatusAndProjectId(TaskStatus status, Long projectId, Pageable pageable);

    List<Task> findByAssigneeId(Long assigneeId);

    @Modifying
    @Query("UPDATE Task t SET t.assignee = null WHERE t.assignee.id = :assigneeId")
    void unassignTasksFromUser(@Param("assigneeId") Long assigneeId);
}
