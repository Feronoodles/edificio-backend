UPDATE apartments
SET number = CONCAT('DEL-', id)
WHERE deleted = TRUE
  AND number NOT LIKE 'DEL-%';

UPDATE residents
SET document_number = CONCAT('DEL-', id)
WHERE deleted = TRUE
  AND document_number NOT LIKE 'DEL-%';
