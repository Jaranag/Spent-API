package com.grupo.spent.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grupo.spent.dtos.requests.CreateEventDto;
import com.grupo.spent.dtos.requests.EditEventDto;
import com.grupo.spent.entities.Event;
import com.grupo.spent.entities.Sport;
import com.grupo.spent.entities.User;
import com.grupo.spent.services.EventService;
import com.grupo.spent.services.SportService;
import com.grupo.spent.services.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/events")
@AllArgsConstructor
public class EventController {

    @Autowired
    private EventService eventService;
    @Autowired
    private SportService sportService;
    @Autowired
    private UserService userService;

    @PostMapping("")
    public ResponseEntity<?> createEvent(@RequestBody CreateEventDto createEventDto) throws Exception {
        try {
            Sport sport = sportService.getSportByName(createEventDto.getSportName());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userService.findUserByUsername(username);

            Event event = eventService.createEvent(
                    createEventDto.getTitle(),
                    createEventDto.getDescription(),
                    createEventDto.getDate(),
                    createEventDto.getStartTime(),
                    createEventDto.getEndTime(),
                    createEventDto.getNumParticipants(),
                    createEventDto.getAddress(),
                    sport,
                    user);
            eventService.joinEvent(event, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(event);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CREATED).body(e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getAllEvents() {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        Event event = eventService.getEventById(id);
        if (event != null && event.getUserCreator().getId().equals(user.getId())) {
            eventService.deleteEvent(id);
            return ResponseEntity.status(HttpStatus.OK).body("Event deleted");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are not the Creator of the event");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editEvent(@PathVariable Integer id, @RequestBody EditEventDto editEventDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        Event event = eventService.getEventById(id);
        if (event != null && event.getUserCreator().getId().equals(user.getId())) {
            String title = editEventDto.getTitle().orElse(event.getTitle());
            String description = editEventDto.getDescription().orElse(event.getDescription());
            LocalDate date = editEventDto.getDate().orElse(event.getDate());
            LocalTime startTime = editEventDto.getStartTime().orElse(event.getStartTime());
            LocalTime endTime = editEventDto.getEndTime().orElse(event.getEndTime());
            Integer numParticipants = editEventDto.getNumParticipants().orElse(event.getNumParticipants());
            String address = editEventDto.getAddress().orElse(event.getAddress());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(eventService.editEvent(id, title, description, date, startTime, endTime, numParticipants, address));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are not the Creator of the event");
    }

    @PostMapping("/join/{id}")
    public ResponseEntity<?> joinEvent(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        Event event = eventService.getEventById(id);

        if (event.getEventParticipants().size() > event.getNumParticipants())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Event already filled.");

        if (eventContainsUser(event, user))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You already joined this event");

        return ResponseEntity.status(HttpStatus.OK).body(eventService.joinEvent(event, user));
    }

    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<?> withdrawEvent(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findUserByUsername(username);
        Event event = eventService.getEventById(id);

        if (!eventContainsUser(event, user))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You haven't joined this event");

        return ResponseEntity.status(HttpStatus.OK).body(eventService.withdrawEvent(event, user));
    }

    public boolean eventContainsUser(Event event, User user) {
        boolean check = false;
        if (event.getEventParticipants().isEmpty())
            return check;
        for (int i = 0; i < event.getEventParticipants().size(); i++) {
            if (event.getEventParticipants().get(i).getId() == user.getId()) {
                check = true;
            }
        }

        return check;
    }

}