import { useState } from "react";
import { useNavigate } from "react-router";
import { Form, Container } from "react-bootstrap";
import { Typeahead } from "react-bootstrap-typeahead";
import "react-bootstrap-typeahead/css/Typeahead.css";
import OnboardingLayout from "main/layouts/OnboardingLayout/OnboardingLayout";
import loginProviderSchools from "main/utils/loginProviderSchools";

export default function OnboardingSchoolSelectionPage() {
  const navigate = useNavigate();
  const [selectedSchool, setSelectedSchool] = useState([]);

  // Combine all schools from all providers with their provider info
  const allSchools = [
    ...loginProviderSchools.google.map((school) => ({
      name: school,
      provider: "google",
    })),
    ...loginProviderSchools.microsoft.map((school) => ({
      name: school,
      provider: "microsoft",
    })),
  ];

  const handleSchoolChange = (selected) => {
    setSelectedSchool(selected);
    if (selected.length > 0) {
      // Store the selected school and provider in session storage
      sessionStorage.setItem("onboardingSchool", selected[0].name);
      sessionStorage.setItem("onboardingProvider", selected[0].provider);
      // Navigate to the sign-in page
      navigate("/onboarding/signin");
    }
  };

  return (
    <OnboardingLayout currentStep={1} totalSteps={5}>
      <Container className="text-center">
        <h1 className="mb-4">Welcome to Frontiers!</h1>
        <hr />
        <p className="mb-4">
          Please pick the school you have credentials with:
        </p>
        <Form.Group
          className="mb-4"
          style={{ maxWidth: "400px", margin: "0 auto" }}
        >
          <Typeahead
            id="school-typeahead"
            options={allSchools}
            labelKey="name"
            placeholder="Start typing to select a school..."
            selected={selectedSchool}
            onChange={handleSchoolChange}
            highlightOnlyResult
            inputProps={{
              "aria-label": "Select School",
              "data-testid": "OnboardingSchoolSelection-typeahead",
            }}
          />
        </Form.Group>
      </Container>
    </OnboardingLayout>
  );
}
