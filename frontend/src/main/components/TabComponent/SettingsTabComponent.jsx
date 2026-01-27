import React from "react";
import CanvasApiForm from "main/components/Settings/CanvasApiForm";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";


export default function SettingsTabComponent({
    courseId,
    testIdPrefix
}) {

    const onSuccessCanvasCredentialsAdded = () => {
        toast("Canvas credentials successfully added.");
    };

    const objectToAxiosParamsCanvasToken = (formData) => ({
        url: `/api/courses/updateCourseCanvasToken`,
        method: "PUT",
        params: {
            courseId: courseId,
            canvasCourseId: formData.canvasCourseId,
            canvasApiToken: formData.canvasApiToken,
            
        },
    });

    const canvasMutation = useBackendMutation(
        objectToAxiosParamsCanvasToken,
        { onSuccess: onSuccessCanvasCredentialsAdded },
    );
    const handleSubmit = (formData) => {
        canvasMutation.mutate(formData);
    }

    return (
        <div data-testid={`${testIdPrefix}-canvasForm`}>
        <CanvasApiForm 
            submitAction ={handleSubmit}
        />
        </div>
    );
}
