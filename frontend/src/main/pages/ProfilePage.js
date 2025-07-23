import { Row, Col, Button } from "react-bootstrap";
import RoleBadge from "main/components/Profile/RoleBadge";
import { hasRole, useCurrentUser } from "main/utils/currentUser";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";

import { Inspector } from "react-inspector";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";
import { useState } from "react";
import ConfirmationModal from "main/components/Common/ConfirmationModal";

const ProfilePage = () => {
  const currentUser = useCurrentUser();
  const onSuccess = (message) => {
    toast(message);
  };

  const disconnectGithub = useBackendMutation(
    () => {
      return {
        url: "/api/github/disconnect",
        method: "DELETE",
      };
    },
    { onSuccess: onSuccess },
    ["current user"],
  );

  const [viewModal, setViewModal] = useState(false);

  if (!currentUser.loggedIn) {
    return <p>Not logged in.</p>;
  }

  const { email, pictureUrl, fullName } = currentUser.root.user;

  return (
    <BasicLayout>
      <ConfirmationModal
        onYes={disconnectGithub.mutate}
        showModal={viewModal}
        setShowModal={setViewModal}
      >
        <p>Are you sure you want to disconnect your Github account?</p>
        <p>Please only do so if you know what you're doing.</p>
      </ConfirmationModal>
      <Row className="align-items-center profile-header mb-5 text-center text-md-left">
        <Col md={2}>
          <img
            src={pictureUrl}
            alt="Profile"
            className="rounded-circle img-fluid profile-picture mb-3 mb-md-0"
          />
        </Col>
        <Col md>
          <h2>{fullName}</h2>
          <p className="lead text-muted">{email}</p>
          <RoleBadge role={"ROLE_USER"} currentUser={currentUser} />
          <RoleBadge role={"ROLE_MEMBER"} currentUser={currentUser} />
          <RoleBadge role={"ROLE_ADMIN"} currentUser={currentUser} />
        </Col>
      </Row>
      <Row className="text-left">
        <Inspector data={currentUser.root} />
      </Row>
      <Row className={"mt-3 g-3"} data-testid={"ProfilePage-advancedFeatures"}>
        <h2>Advanced Features</h2>
        {hasRole(currentUser, "ROLE_GITHUB") && (
          <>
            {" "}
            <Col md={2}>
              <Button variant={"danger"} onClick={() => setViewModal(true)}>
                Disconnect GitHub
              </Button>
            </Col>
          </>
        )}
      </Row>
    </BasicLayout>
  );
};

export default ProfilePage;
