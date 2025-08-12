# Requirements Document

## Introduction

The Movie Matcher app is a Tinder-style mobile application that allows couples to discover movies they both want to watch together. Users swipe through movie recommendations, and when both partners "like" the same movie, it creates a match. The app integrates with movie databases and streaming providers to show availability and help couples decide what to watch tonight.

## Requirements

### Requirement 1

**User Story:** As a couple, I want to create a shared room where both of us can swipe on movies, so that we can find films we both want to watch together.

#### Acceptance Criteria

1. WHEN a user creates a new room THEN the system SHALL generate a unique room ID and invite code
2. WHEN a user shares an invite code THEN their partner SHALL be able to join the room using that code
3. WHEN both users are in a room THEN the system SHALL limit room membership to exactly two users
4. WHEN a room is created THEN the system SHALL store both users' profiles and preferences

### Requirement 2

**User Story:** As a user, I want to swipe through movie recommendations in a Tinder-style interface, so that I can quickly indicate my interest in different films.

#### Acceptance Criteria

1. WHEN a user opens the swipe screen THEN the system SHALL display movie cards with poster, title, year, rating, and streaming availability
2. WHEN a user swipes right or taps like THEN the system SHALL record a "like" decision for that movie
3. WHEN a user swipes left or taps pass THEN the system SHALL record a "pass" decision for that movie
4. WHEN a user makes a swipe decision THEN the system SHALL immediately show the next movie card
5. WHEN the movie queue runs low THEN the system SHALL automatically fetch more movie recommendations

### Requirement 3

**User Story:** As a user, I want to see real-time matches when both my partner and I like the same movie, so that we can discover films we both want to watch.

#### Acceptance Criteria

1. WHEN both users like the same movie THEN the system SHALL create a match within 1 second
2. WHEN a match is created THEN the system SHALL send push notifications to both users
3. WHEN a match occurs THEN the system SHALL add the movie to the shared matches list
4. WHEN viewing matches THEN the system SHALL display movie details and streaming provider availability

### Requirement 4

**User Story:** As a user, I want to filter movie recommendations based on preferences like genre, year, and streaming providers, so that I only see relevant content.

#### Acceptance Criteria

1. WHEN users set preferences THEN the system SHALL filter recommendations by selected genres, year range, and minimum rating
2. WHEN users select streaming providers THEN the system SHALL prioritize movies available on those platforms
3. WHEN "availability strict" is enabled THEN the system SHALL only show movies available on selected providers
4. WHEN preferences are updated THEN the system SHALL apply filters to future recommendations immediately

### Requirement 5

**User Story:** As a user, I want to undo my last swipe decision, so that I can correct mistakes or reconsider my choice.

#### Acceptance Criteria

1. WHEN a user requests undo THEN the system SHALL reverse their most recent swipe decision
2. WHEN an undo is performed THEN the system SHALL remove the swipe record from the database
3. WHEN an undo would affect an existing match THEN the system SHALL also remove the match
4. WHEN no recent swipe exists THEN the system SHALL disable the undo option

### Requirement 6

**User Story:** As a user, I want to see streaming provider availability for movies, so that I know where I can watch matched films.

#### Acceptance Criteria

1. WHEN displaying movie cards THEN the system SHALL show available streaming providers with recognizable badges
2. WHEN viewing matches THEN the system SHALL display all available streaming options
3. WHEN a user taps a provider badge THEN the system SHALL attempt to open the movie in that streaming app
4. WHEN a deep link fails THEN the system SHALL fallback to the provider's website

### Requirement 7

**User Story:** As a user, I want the app to suggest tonight's pick from our matches, so that we can quickly decide what to watch.

#### Acceptance Criteria

1. WHEN requesting a suggestion THEN the system SHALL recommend the highest-rated unwatched match
2. WHEN multiple movies have the same rating THEN the system SHALL prioritize more recent releases
3. WHEN "availability strict" is enabled THEN the system SHALL only suggest movies on selected providers
4. WHEN no matches meet criteria THEN the system SHALL display an appropriate message

### Requirement 8

**User Story:** As a user, I want to mark matches as watched and add notes, so that I can track what we've seen and remember our thoughts.

#### Acceptance Criteria

1. WHEN viewing a match THEN the system SHALL provide options to mark as watched and add notes
2. WHEN a match is marked watched THEN the system SHALL exclude it from future suggestions
3. WHEN notes are added THEN the system SHALL save and display them with the match
4. WHEN viewing match history THEN the system SHALL show watched status and notes

### Requirement 9

**User Story:** As a user, I want my data to be automatically cleaned up after 90 days, so that old swipes and matches don't clutter the system.

#### Acceptance Criteria

1. WHEN swipes or matches are older than 90 days THEN the system SHALL automatically delete them
2. WHEN a user requests to clear their swipes THEN the system SHALL delete all their swipe history in the current room
3. WHEN data cleanup runs THEN the system SHALL preserve user profiles and room information
4. WHEN cleanup occurs THEN the system SHALL not affect matches marked as watched with notes

### Requirement 10

**User Story:** As a user, I want to authenticate securely and maintain my profile across sessions, so that my preferences and history are preserved.

#### Acceptance Criteria

1. WHEN accessing the app THEN the system SHALL require Google Sign-In authentication
2. WHEN a user signs in THEN the system SHALL create or update their profile with display name and photo
3. WHEN a user signs in THEN the system SHALL register their device for push notifications
4. WHEN a user returns to the app THEN the system SHALL restore their room membership and preferences