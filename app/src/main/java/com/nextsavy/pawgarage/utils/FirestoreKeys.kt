package com.nextsavy.pawgarage.utils

class CollectionUser {
    companion object {
        const val name = "Users"
        const val kUserName = "name"
        const val kContactNumber = "contact_number"
        const val kUserType = "user_type"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
        const val kCountryCode = "country_code"

        // Constants
        const val COUNTRY_CODE = "+91"

    }
}
class CollectionWhitelistedNumbers {
    companion object {
        const val name = "Whitelisted Numbers"
        const val kContactNumber = "contact_number"
        const val kUserType = "user_type"
        const val kUserName = "user_name"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        const val ADMIN = "Admin"
        const val TEAM_LEADER = "Team Leader"
        const val TEAM_MEMBER = "Team Member"
    }
}
class CollectionReportingPersons {
    companion object {
        const val name = "Reporting Persons"
        const val kContactNumber = "contact_number"
        const val kUserName = "user_name"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionReminders {
    companion object {
        const val name = "Reminders"
        const val kReminderTypeObjectId = "reminder_type_object_id"
        const val kReminderDate = "reminder_date"
        const val kReminderType = "reminder_type"
        const val kAnimalDocId = "animal_doc_id"
        const val kIsComplete = "is_complete"
        const val kIsArchive = "is_archive"
        const val kIsTurnedOff = "is_turned_off"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        const val VACCINATION = "Vaccination"
        const val DEWORMING = "Deworming"
        const val COMPLETE_PROFILE = "Complete profile"
    }
}

class CollectionFeedEvents {
    companion object {
        const val name = "FeedEvents"
        const val kAnimalDocId = "animal_doc_id"
        const val kFeedType = "feed_type"
        const val kFeedObjectId = "feed_object_id"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionProfileLeads {
    companion object {
        const val name = "Profile Leads"
        const val kAnimalId = "animal_id"
        const val kName = "name"
        const val kDownloadUrl = "download_url"
        const val kLatitude = "latitude"
        const val kLongitude = "longitude"
        const val kLocationAddress = "location_address"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
    }
}

class CollectionAnimals {
    companion object {
        const val name = "Animals"
        const val kAnimalId = "animal_id"
        const val kIsDead = "is_dead"
        const val kIsTerminated = "is_terminated"
        const val kName = "name"
        const val kSearchKeywords = "search_keywords"
        const val kType = "type"
        const val kState = "state"
        const val kDescription = "description"
        const val kWeight = "weight"
        const val kGender = "gender"
        const val kSpecies = "species"
        const val kDownloadUrl = "download_url"
        const val kLatitude = "latitude"
        const val kLongitude = "longitude"
        const val kLocationAddress = "location_address"
        const val kDate = "date"
        const val kAdmissionDate = "admission_date"
        const val kReportingPerson = "reporting_person"
        const val kContactNumberWithCode = "contact_number"
        const val kMedicalConditions = "medical_conditions"
        const val kIsMedicalConditionsApplicable = "is_medical_conditions_applicable"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        // Constants
        const val IPD = "IPD"
        const val OPD = "OPD"
        const val DOG = "Dog"
        const val CAT = "Cat"
        const val OTHER = "Other"
        const val MALE = "Male"
        const val FEMALE = "Female"
        const val ACTIVE = "Active"
        const val TERMINATED = "Terminated"
    }
}

class CollectionArchived {
    companion object {
        const val name = "Archived Animals"
        const val kAnimalDocId = "animal_doc_id"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionVaccination {
    companion object {
        const val name = "Vaccination"
        const val kName = "name"
        const val kAnimalId = "animal_id"
        const val kAnimalDocId = "animal_doc_id"
        const val kDurationType = "duration_type"
        const val kVaccinationDate = "vaccination_date"
        const val kVaccineType = "vaccine_type"
        const val kPersonAdministratedId = "person_administrated_id"
        const val kVaccinationStatus = "vaccination_status"
        const val kAdminNote = "admin_note"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        const val PENDING = "Pending"
        const val COMPLETED = "Completed"
        const val TERMINATED = "Terminated"

        const val DURATION_DAYS = "21 Days"
        const val DURATION_YEAR = "365 Days"
    }
}
class CollectionDeworming {
    companion object {
        const val name = "Deworming"
        const val kAnimalId = "animal_id"
        const val kAnimalDocId = "animal_doc_id"
        const val kDurationType = "duration_type"
        const val kDewormingDate = "deworming_date"
        const val kMedicineType = "medicine_type"
        const val kWeight = "weight"
        const val kPersonAdministratedId = "person_administrated_id"
        const val kDewormingStatus = "deworming_status"
        const val kAdminNote = "admin_note"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        const val PENDING = "Pending"
        const val COMPLETED = "Completed"
        const val TERMINATED = "Terminated"
        const val DURATION_30 = "30 Days"
        const val DURATION_90 = "90 Days"
        const val DURATION_YEAR = "365 Days"
    }
}
class CollectionRelease {
    companion object {
        const val name = "Released"
        const val kAnimalId = "animal_id"
        const val kAnimalDocId = "animal_doc_id"
        const val kReleasedDate = "released_date"
        const val kReleasedStatus = "released_status"
        const val kAdopterId = "adopter_id"
        const val kAdopterName = "adopter_name"
        const val kCountryCode = "country_code"
        const val kContactNumber = "contact_number"
        const val kLatitude = "latitude"
        const val kLongitude = "longitude"
        const val kLocationAddress = "location_address"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
        const val kComment = "comment"

        const val DEATH = "Death"
    }
}
class CollectionAdmission {
    companion object {
        const val name = "Admission"
        const val kAnimalId = "animal_id"
        const val kAnimalDocId = "animal_doc_id"
        const val kWeight = "weight"
        const val kAdmissionDate = "admission_date"
        const val kReportingPerson = "reporting_person"
        const val kReportingPersonId = "reporting_person_id"
        const val kCountryCode = "country_code"
        const val kContactNumber = "contact_number"
        const val kMedicalConditions = "medical_conditions"
        const val kMedicalConditionIds = "medical_condition_ids"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionTreatment {
    companion object {
        const val name = "Treatment"
        const val kAnimalId = "animal_id"
        const val kAnimalDocId = "animal_doc_id"
        const val kTreatmentDate = "treatment_date"
        const val kReportingPerson = "reporting_person"
        const val kReportingPersonId = "reporting_person_id"
        const val kCountryCode = "country_code"
        const val kContactNumber = "contact_number"
        const val kMedicalConditions = "medical_conditions"
        const val kMedicalConditionIds = "medical_condition_ids"
        const val kIsMedicalConditionsApplicable = "is_medical_conditions_applicable"
        const val kAdminNote = "admin_note"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionMedicalConditionsList {
    companion object {
        const val name = "Medical Conditions List"
        const val kName = "name"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionVaccinesList {
    companion object {
        const val name = "Vaccines List"
        const val kName = "name"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionScheduleStatus {
    companion object {
        const val name = "Schedule Status List"
    }
}
class CollectionMedicinesList {
    companion object {
        const val name = "Deworming Medicines List"
        const val kName = "name"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}
class CollectionReleaseStatus {
    companion object {
        const val name = "Release Status List"
        const val ADOPTED = "Adopted"
        const val RELEASED = "Released"
        const val DEATH = "Death"
    }
}
class CollectionNotifications {
    companion object {
        const val name = "Notifications"
        const val kNotificationTypeObjectId = "notification_type_object_id"
        const val kNotificationDate = "notification_date"
        const val kNotificationType = "notification_type"
        const val kNotificationSubtype = "notification_subtype"
        const val kAnimalDocId = "animal_doc_id"
        const val kIsComplete = "is_complete"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"

        const val TOPIC = "animal_events"

        const val VACCINATION = "Vaccination"
        const val DEWORMING = "Deworming"
        const val RELEASED = "Released"
        const val ADOPTED = "Adopted"
        const val DEATH = "Death"
        const val TREATMENT = "Treated"
        const val NEW_PROFILE = "New profile"
        const val PROFILE_LEADS = "Profile Leads"
        const val ACTIVATED = "Activated"
        const val TERMINATED = "Terminated"
    }
}

class CollectionAdopters {
    companion object {
        const val name = "Adopters"
        const val kContactNumber = "contact_number"
        const val kUserName = "user_name"
        const val kSearchKeywords = "search_keywords"
        const val kIsArchive = "is_archive"
        const val kCreatedAt = "created_at"
        const val kCreatedBy = "created_by"
        const val kUpdatedAt = "updated_at"
        const val kUpdatedBy = "updated_by"
    }
}

class SharedPrefKeys {
    companion object {
        const val kUserType = "USER_TYPE"
        const val kUserName = "USER_NAME"
    }
}